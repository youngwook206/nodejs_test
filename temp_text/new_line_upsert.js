const express = require('express');
const router = express.Router();
const path = require('path');
const filename = path.basename(__filename);
const approot = require('app-root-path');
const util = require(approot + '/util/util');
const es = require(approot + '/util/es');

// elapsed time
let elapsed = {};
let start, end;

//***********************************************************************************************************
//  Process Logic : 메뉴 보기
//***********************************************************************************************************
/* GET users listing. */
router.get('/', function(req, res) {
	sample_handler(req, res);
});

/* POST users listing. */
router.post('/', function(req, res) {
	sample_handler(req, res);
});


// -- promiss handler
let sample_handler = function(req,res){
	util.req_param('[조회] 줄바꿈 찾기',req,filename);
	elapsed = {};
	start = new Date();
	if(req.method == "POST") req.query = req.body;

	Promise
	.all([sample_paramcheck(req,res)])
	.then(function(){return _promise_checktime('paramcheck');})
	.then(function(){return sample_work(req,res);})
	.then(function(){return _promise_checktime('menu_view');})
	.then(function(){return sample_sendresult(req,res);})
	.catch(function(err){return _promise_errhandler(req,res,err);})
	;
};

let sample_work = async(req,res)=>{
	console.debug('[new_line] start');

	let s_result = [];
	let s_index = "crawl-kistep-yearbook";	//search_index
	let set_bodys = {"body":[]};		// bulk body set

	let searchEs =()=> {
		return new Promise(function(resolve,reject) {
			console.debug('[searchTrLog] req.query.domain_id : ' + req.query.domain_id);

			let s_body;
			let size = 9999;
			s_body =
			{
			  "_source": "attachcontent",
			  "size": size
			};

			es.client1.search({
				index: s_index,
				body: s_body
			}).then(function (resp) {
				if(resp.hits.total>0) {
					s_result = resp.hits.hits;
					console.debug('[searchTrLog] count : ' + resp.hits.total);
				}
				return resolve();
			});
		});
	}

	let findAndUpdate =()=> {
		return new Promise(function(resolve,reject) {
			console.debug('[findAndUpdate] Start');

			let count_idx=0;
			s_result.forEach(result_src => {
				count_idx++;
				console.log(`count_idx => ${count_idx}`);
				
				// reference data variable
				let ref_textset = [];

				// bulkInput item
				let body_item = {};
				let source = {"doc":{}, "doc_as_upsert" : true};
				body_item =
				{
					"_index": s_index,
					"_type": 'contents',
					"_id":result_src._id
				}

				let fieldRes = result_src._source;
				let attachcontent = fieldRes.attachcontent;
				let con_arr = attachcontent.split("\n");
				con_arr.forEach( el=> {
					//single
					ref_delimiters.forEach(s_el => {
						let testReg = new RegExp(s_el, 'g');
						if(testReg.test(el)) {
							console.log(`result_src._id ==> ${result_src._id}`);
							ref_textset = getValidText(ref_textset, el, s_el);
						}
					});

				});
				source.doc['references'] = ref_textset;
				set_bodys.body.push({"update":body_item});
				set_bodys.body.push(source);
			});
			resolve();
		});
	}

	function getValidText( totalArr, reqStr, delimiter ) {
		let s_arr = reqStr.split(delimiter);
		let idx=0;
		s_arr.forEach( s_arr_el => {
			if(idx>0) {
				console.log(`라인 텍스트 ==> ${reqStr}`);
				console.log(`${delimiter}로 구분된 문자열 = ${s_arr_el}`);
				totalArr.push(s_arr_el.trim());
			}
			idx++;
		})
		return totalArr;
	}

	let updateIndex=()=>{
		return new Promise(function(resolve, reject){
			console.log('[updateIndex] Start');

			es.client1.bulk(set_bodys)
			.then(function (resp) {
				console.log('END bulk ~~~~~');
				body = {result:resp};
				return resolve();
			}, function(err){
				err.status = 400;
				res.status(400).send(util.res_err(req, 400, err.message));
				console.error(err.message,filename);
				return resolve();
			});
		});
	}

	await searchEs();
	await findAndUpdate();
	await updateIndex();

}

let sample_paramcheck = function(req,res){
	return new Promise(function(resolve, reject){
		console.debug('[menumatch] sample_paramcheck - start');
		let err = req.validationErrors();
		if(err) {
			err.status = 400;
			res.status(400).send(util.res_err(req, 400, err[0].msg));
			console.error(err[0].msg,filename);
			return reject();
		}else{
			req.query.domain_id = req.query.domain_id || "";
			console.debug('[sample_paramcheck] req.query.domain_id : ' + req.query.domain_id);
			return resolve();
        }
	});
};

let sample_sendresult = async(req,res)=>{
	console.debug('[sample] sample_sendresult - start');

};


//***********************************************************************************************************
//  Process Logic Area (E)
//***********************************************************************************************************

let _promise_checktime = function(name){
	return new Promise(function(resolve, reject){
        // elapsed time
        end = new Date();
        elapsed[name] = (end - start) + ' ms';
        console.debug('_promise_checktime ! - '+name+' ['+elapsed[name]+']');
        return resolve();
    });
};

let _promise_errhandler = function(req,res,err){
	return new Promise(function(resolve, reject){
		if(typeof(err) != 'undefined'){
			console.error(err,filename);
		    //res.status(500).send(util.res_err(req, 500, err.message));

		    res.status(err.status || 500);
		    res.render('error', {
		    	message: err.message,
		    	error: err
		    });
		    return resolve();
		}else{
			return resolve();
		}
	});
};

module.exports = router;
