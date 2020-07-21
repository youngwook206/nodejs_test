// 옵션 ------------------------------------------------------------
// 디버그 설정
var debug = {
  crawl_document: true, // 문서 수집 여부
  move_page: true, // 페이지 이동 여부
  hide_chrome: true, // 크롬 브라우져 숨김
};

// 타임아웃 설정
var timeout = {
  global: 100, // 함수 이동 시 타임아웃
  ready: 3 * 1000, // 첫페이지 이동 후 타임아웃
  move: {
    document: 3 * 1000, // 수집 대상 문서 이동 후 타임아웃 (3*1000)
    document_back: 1 * 1000, // 문서 수집 후 타임아웃
    page: 3 * 1000, // 페이지 이동 후 타임아웃
    next_paging: 5 * 1000, // 다음 페이징 이동 후 타임아웃
  },
  login: 20000,
};

var now_page_idx = 0;
var doc_limit_idx = 0; // 페이지 당 수집 게시물 건수 idx
var page_limit_idx = 0; // 수집할 페이지 idx
var test_next_page_check = true;

// ElasticSearch Client
var esclient = require('../../../esclient');

var fs = require('fs');
var http = require('https');
var Path = require('path');
var Axios = require('axios');

// 로그
var log = require('../../logger');

// 템플릿
var template = {
  history: require('../../../settings/template/history'),
  contents: require('../../../settings/template/contents'),
  seed_settings: require('../../../settings/template/setting_seed'),
};

// moment
var moment = require('moment');

// md5
var md5 = require('md5');

// 웹드라이버 PATH를 환경 변수에 추가
if (process.platform.toLowerCase() == 'win32') {
  process.env.Path += ';' + __dirname + '/../../../../webdriver';
  log.info('SET Path:', process.env.Path);
} else {
  // darwin, linux2
  process.env.PATH += ':' + __dirname + '/../../../../webdriver';
  log.info('SET PATH:', process.env.PATH);
}

// 웹드라이버
var webdriver = require('selenium-webdriver');

// Chrome 옵션
var chromeCapabilities = webdriver.Capabilities.chrome();
var chromeOptions = {
  args: [],
};
if (debug.hide_chrome === true) {
  chromeOptions.args.push('--window-size=1920,1080');
  chromeOptions.args.push('--disable-extensions');
  chromeOptions.args.push("--proxy-server='direct://'");
  chromeOptions.args.push('--proxy-bypass-list=*');
  chromeOptions.args.push('--start-maximized');
  chromeOptions.args.push('--headless');
  chromeOptions.args.push('--disable-gpu');
  chromeOptions.args.push('--disable-dev-shm-usage');
  chromeOptions.args.push('--no-sandbox');
  chromeOptions.args.push('--ignore-certificate-errors');
}

// 크롬드라이버
chromeCapabilities.set('chromeOptions', chromeOptions);
var driver;

// 상태 정보
var status = require('../status');

// seed import
// var seed = require('./seed/'+argv.seed);
//######################### TO-BE logic #############################

var seed;
var start = function (target) {
  log.info('wcrawler started.');
  seed = target;
  driver = new webdriver.Builder().forBrowser('chrome').withCapabilities(chromeCapabilities).build();

  var paramCon = {
    running: true,
  };
  updateSeedInfo(paramCon);

  //로그인 필요시 로그인 요청(**시드정보로 대체할 것!!)
  // var By = webdriver.By;
  // var until = webdriver.until;

  // driver.get('http://www.badaweb.co.kr/bada3/bbs/login.php');
  // driver.wait(until.elementLocated(By.xpath('//*[@id="login_id"]')), timeout.login).sendKeys('@@@@');
  // driver.wait(until.elementLocated(By.xpath('//*[@id="login_pw"]')), timeout.login).sendKeys('@@@@');
  // driver.wait(until.elementLocated(By.xpath('//*[@id="login_fs"]/input[3]')), timeout.login).click();

  driver
    .get(seed.url)
    .then(updateHistory)
    .then(function (result) {
      status.elasticsearch.id = result._id;
      return;
    })
    .then(build.lists)
    .catch(function (err) {
      log.error('crawling error- build is failed.');
      stop();
    });
};

var updateHistory = function () {
  // console.log(JSON.stringify(seed, null, 2));
  // 상태정보 설정
  status.seed = seed.id;
  status.url = seed.url;
  // status.stat.start = moment().add(9, 'hours').utc().toISOString();
  status.stat.start = moment().utc().toISOString();
  status.stat.end = status.stat.start;

  log.info(
    'crawler started: ' +
      JSON.stringify({
        seed: seed.url,
      })
  );

  // 히스토리 업데이트
  var params = {
    index: template.history.index,
    type: 'history',
    body: {
      seed: status.seed,
      url: status.url,
      start: status.stat.start,
      end: status.stat.end,
      counts: 0,
    },
  };
  return esclient.index(params).catch(function (err) {
    log.error('cannot update index history: ' + JSON.stringify(err));
    log.error('crawling is stopped');
    stop();
  });
};

var build = {
  lists: function () {
    log.info('building lists started');

    // console.log(JSON.stringify(seed, null, 2));
    // 리스트 수집
    var selector = webdriver_selector(seed.lists);

    driver
      .findElements(selector)
      .then(function (resp) {
        resp.map(function (ele) {
          var doc = {};
          var attachment = {};
          var selector = webdriver_selector(seed.lists.target);
          ele
            .findElements(selector)
            .then(function (resp) {
              // target 수집
              if (resp && resp.length > 0) {
                if (seed.lists.target.action.toLowerCase() == 'href') {
                  return resp[0].getAttribute('href');
                } else if (seed.lists.target.action.toLowerCase() == 'javascript_href') {
                  return resp[0].getAttribute('href');
                } else if (seed.lists.target.action.toLowerCase() == 'javascript_onclick') {
                  return resp[0].getAttribute('onclick');
                } else if (seed.lists.target.action.toLowerCase() == 'nowpage') {
                  // $$$$$$$$$$$$$$$$$$$$$$$$$$$
                  return resp[0].getText();
                } else {
                  log.error(
                    'unknown action type: ' +
                      JSON.stringify({
                        where: 'seed.lists.target.action',
                        action: seed.lists.target.action,
                      })
                  );
                  setTimeout(error, timeout.global);
                }
              }
              return undefined;
            })
            .then(function (resp) {
              if (resp) {
                if (seed.lists.target.action.toLowerCase() == 'nowpage') {
                  doc.target = resp + '_' + now_page_idx;
                } else {
                  doc.target = resp;
                }
              }
              return undefined;
            })
            .then(function () {
              // list상의 필드 수집
              Object.keys(seed.lists).forEach(function (key) {
                var selector = undefined;
                if (key == 'thumbnail' && seed.lists[key]) {
                  selector = webdriver_selector(seed.lists[key]);

                  ele
                    .findElements(selector)
                    .then(function (resp) {
                      if (resp.length > 0) {
                        // console.log('resp[0].getText() ::: ' + resp[0].getText());
                        return resp[0].getAttribute('src');
                      }
                      return undefined;
                    })
                    .then(function (rtn) {
                      // console.log('list thumbnail ::: ' + rtn);
                      doc[key] = rtn;
                    });
                } else if (key != 'type' && key != 'value' && key != 'target' && key != 'attachments' && seed.lists[key]) {
                  // console.log('key ::: ' + key + ' || seed.lists[key] ::: ' + JSON.stringify(seed.lists[key], null, 2));
                  selector = webdriver_selector(seed.lists[key]);

                  ele
                    .findElements(selector)
                    .then(function (resp) {
                      if (resp.length > 0) {
                        // console.log('resp[0].getText() ::: ' + resp[0].getText());
                        return resp[0].getText();
                      }
                      return undefined;
                    })
                    .then(function (rtn) {
                      // console.log('rtn ::: ' + rtn);
                      // if (key == 'timestamp') {
                      //     doc[key] = moment(rtn, seed.lists.timestamp.format);

                      if (seed.lists[key].format) {
                        doc[key] = moment(rtn, seed.lists[key].format).format(seed.lists[key].format);
                      } else {
                        doc[key] = rtn;
                      }
                    });
                }
              });

              return undefined;
            })
            .then(function () {
              // attachment name 수집

              if (seed.lists.attachment && seed.lists.attachment.name) {
                var selector = webdriver_selector(seed.lists.attachment.name);

                return ele.findElements(selector);
              }
              return undefined;
            })
            .then(function (resp) {
              if (resp && resp.length > 0) {
                if (seed.lists.attachment.name.extract == 'text') {
                  return resp[0].getText();
                } else {
                  log.error(
                    'unknown attachment name extract: ' +
                      JSON.stringify({
                        where: 'seed.lists.attachment.name',
                        extract: seed.lists.attachment.name.extract,
                      })
                  );
                  setTimeout(error, timeout.global);
                }
              }
              return undefined;
            })
            .then(function (resp) {
              if (resp) {
                attachment.name = resp;
              }
              return undefined;
            })
            .then(function () {
              // attachment url 수집
              if (seed.lists.attachment && seed.lists.attachment.url) {
                var selector = webdriver_selector(seed.lists.attachment.url);
                return ele.findElements(selector);
              }
              return undefined;
            })
            .then(function (resp) {
              if (resp && resp.length > 0) {
                if (seed.lists.attachment.url.extract == 'attribute_href') {
                  return resp[0].getAttribute('href');
                } else if (seed.lists.attachment.url.extract == 'attribute_onclick') {
                  return resp[0].getAttribute('onclick');
                } else {
                  log.error(
                    'unknown attachment url extract: ' +
                      JSON.stringify({
                        where: 'seed.lists.attachment.url',
                        extract: seed.lists.attachment.url.extract,
                      })
                  );
                  setTimeout(error, timeout.global);
                }
              }
              return undefined;
            })
            .then(function (resp) {
              if (resp) {
                attachment.url = resp;
              }
              return undefined;
            })
            .then(function (resp) {
              if (resp && resp.length > 0) {
                doc.attachments = doc.attachments || [];
                doc.attachments.push(attachment);
              }

              if (doc.target) {
                log.debug('detect target: ' + JSON.stringify(doc));

                if (!status.visit.lists[doc.target]) {
                  // status.target.lists.push(doc);       $$$$$$$$$$$$$$$$$$$$$$$$$$
                  if (seed.test_doc_per_page > 0 && doc_limit_idx < seed.test_doc_per_page) {
                    // test_doc_per_page 가 0 이상이면 리스트의 value 만큼의 문서만 수집
                    status.target.lists.push(doc);
                    doc_limit_idx++;
                  } else if (seed.test_doc_per_page == 0) {
                    // test_doc_per_page 가 0이면 리스트의 모든 문서 수집
                    status.target.lists.push(doc);
                  }
                  log.info('add target to lists: ' + JSON.stringify(doc));
                }

                if (debug.crawl_document === false) {
                  status.visit.lists[doc.target] = true;
                }
              }
            });
        });
      })
      .then(function () {
        log.debug('building lists ended');
        //crawl.lists.bind(null, seed);

        setTimeout(crawl.lists, timeout.global);
      })
      .catch(function (err) {
        log.error('building lists error: ' + JSON.stringify(err));
        setTimeout(error, timeout.global);
      });
  },
  pages: function () {
    if (seed.paging) {
      log.debug('building pages started');

      // 페이징 목록
      var selector = webdriver_selector(seed.paging);
      log.info('pages: ' + JSON.stringify(selector));
      driver
        .findElements(selector)
        .then(function (paging) {
          if (seed.paging.ignores && seed.paging.ignores !== 'undefined') {
            // 페이지 별 제외 대상 패턴을 검사
            var pages_promise_all = [];
            paging.forEach(function (ele) {
              var ignore_promise_all = [];
              seed.paging.ignores.forEach(function (ignore) {
                var selector = webdriver_selector(ignore);
                ignore_promise_all.push(
                  ele.findElements(selector).then(function (resp) {
                    if (resp.length > 0) {
                      return false;
                    }
                    return true;
                  })
                );
              });

              pages_promise_all.push(
                webdriver.promise.all(ignore_promise_all).then(function (resp) {
                  return resp.every(function (e) {
                    return e;
                  });
                })
              );
            });

            // 제외 대상을 제거한 페이지 정보를 반환
            return webdriver.promise.all(pages_promise_all).then(function (resp) {
              var pages = [];
              for (var i in resp) {
                if (paging[i]) {
                  if (resp[i] === true) {
                    pages.push(paging[i]);
                  }
                }
              }
              return pages;
            });
          } else {
            return paging;
          }
        })
        .then(function (resp) {
          // 페이지 상세 정보 수집
          var get_pages_all = [];
          resp.forEach(function (ele) {
            if (seed.paging.action.toLowerCase() == 'href') {
              get_pages_all.push(ele.getAttribute('href'));
            } else if (seed.paging.action.toLowerCase() == 'javascript_href') {
              get_pages_all.push(ele.getAttribute('href'));
            } else if (seed.paging.action.toLowerCase() == 'javascript_onclick') {
              get_pages_all.push(ele.getAttribute('onclick'));
            } else {
              log.error(
                'unknown action type: ' +
                  JSON.stringify({
                    where: 'seed.paging.action',
                    action: seed.paging.action,
                  })
              );
              setTimeout(error, timeout.global);
            }
          });

          return webdriver.promise.all(get_pages_all).then(function (resp) {
            return resp;
          });
        })
        .then(function (resp) {
          // 수집된 페이지 추가
          resp.forEach(function (ele) {
            if (ele) {
              log.debug(
                'detect page: ' +
                  JSON.stringify({
                    page: ele,
                  })
              );

              if (!status.visit.pages[ele]) {
                // status.target.pages.push(ele);
                if (seed.test_max_page_num > 0 && page_limit_idx < seed.test_max_page_num - 1) {
                  // test_max_page_num 가 0 이상이면 리스트의 value 만큼의 문서만 수집
                  status.target.pages.push(ele);
                  page_limit_idx++;
                  log.info(
                    'add page to pages: ' +
                      JSON.stringify({
                        page: ele,
                      })
                  );
                } else if (seed.test_max_page_num == 0) {
                  // test_max_page_num 가 0이면 리스트의 모든 문서 수집
                  status.target.pages.push(ele);
                  log.info(
                    'add page to pages: ' +
                      JSON.stringify({
                        page: ele,
                      })
                  );
                }
                // log.info('add page to pages: ' + JSON.stringify({
                //     page: ele
                // }));
              }

              if (debug.move_page === false) {
                status.visit.pages[ele] = true;
              }
            }
          });
          log.debug('building pages ended');

          if (status.target.pages.length > 0) {
            doc_limit_idx = 0;
            setTimeout(move.page, timeout.global);
          } else {
            setTimeout(move.next_paging, timeout.global);
          }
        })
        .catch(function (err) {
          log.error('building pages error: ' + JSON.stringify(err));
          setTimeout(error, 100);
        });
    } else {
      setTimeout(move.next_paging, timeout.global);
    }
  },
};
var crawl = {
  lists: function () {
    // 수집된 문서는 제외
    var doc;
    while ((doc = status.target.lists.shift())) {
      if (status.visit.lists[doc.target]) {
        continue;
      }
      break;
    }

    // console.log('doc ::: ' + JSON.stringify(doc, null, 2));
    if (doc && doc.target && doc.target.length > 0) {
      if (seed.lists.target.move_view === undefined || seed.lists.target.move_view === true) {
        // 문서가 있는 경우 문서 수집

        if (seed.lists.target.action.toLowerCase() == 'href') {
          driver
            .navigate()
            .to(doc.target)
            .then(function () {
              setTimeout(crawl.document, timeout.move.document, doc);
            });
        } else if (seed.lists.target.action.toLowerCase() == 'javascript_href') {
          return driver.executeScript(doc.target).then(function () {
            setTimeout(crawl.document, timeout.move.document, doc);
          });
        } else if (seed.lists.target.action.toLowerCase() == 'javascript_onclick') {
          return driver.executeScript(doc.target).then(function () {
            setTimeout(crawl.document, timeout.move.document, doc);
          });
        } else if (seed.lists.target.action.toLowerCase() == 'nowpage') {
          // $$$$$$$$$$$$$$$$$$$$$$$$$$
          // return driver.executeScript(doc.target).then(function() {
          setTimeout(crawl.document, timeout.move.document, doc);
          // });
        } else {
          log.error(
            'unknown action type: ' +
              JSON.stringify({
                where: 'seed.lists.target.action',
                action: seed.lists.target.action,
              })
          );
          setTimeout(error, timeout.global);
        }
      } else {
        setTimeout(crawl.document, timeout.move.document, doc);
      }
    } else {
      // 문서가 없는 경우 페이징 탐색
      doc_limit_idx = 0;
      setTimeout(move.page, timeout.global);
    }
  },
  document: function (doc) {
    log.debug('crawling document started: ' + JSON.stringify(doc));
    //alert창 있을 시 확인 요청
    driver
      .switchTo()
      .alert()
      .then(function () {
        driver.switchTo().alert().accept();
      })
      .catch(function () {
        return;
      });

    // 수집 문서로 기록
    status.visit.lists[doc.target] = true;

    // 문서 객체 복사
    var cdoc = JSON.parse(JSON.stringify(doc));

    // 문서 수집
    webdriver.promise
      .fulfilled()
      .then(function () {
        Object.keys(seed.view).forEach(function (key) {
          if (key == 'thumbnail' && seed.view[key]) {
            var selector = webdriver_selector(seed.view[key]);
            driver
              .findElements(selector)
              .then(function (resp) {
                if (resp.length > 0) {
                  return resp[0].getAttribute('src');
                }
                return undefined;
              })
              .then(function (rtn) {
                cdoc[key] = rtn;
              });
          } else if (
            key != 'type' &&
            key != 'value' &&
            key != 'target' &&
            key != 'attachments' &&
            seed.view[key] &&
            seed.view[key] != 'undefined'
          ) {
            // console.log("key --> " + key);
            var selector = webdriver_selector(seed.view[key]);
            driver
              .findElements(selector)
              .then(function (resp) {
                if (resp.length > 0) {
                  return resp[0].getText();
                }
                return undefined;
              })
              .then(function (rtn) {
                // if (key == 'timestamp') {
                //     cdoc[key] = moment(rtn, seed.view.timestamp.format);
                if (seed.view[key].format) {
                  cdoc[key] = moment(rtn, seed.view[key].format).format(seed.view[key].format);
                } else {
                  cdoc[key] = rtn;
                }
              });
          } else if (seed.view[key] == undefined || seed.view[key] == 'undefined') {
            cdoc[key] = '';
          }
        });

        // console.log('doc ::: ' + JSON.stringify(doc, null, 2));
        return undefined;
      })
      .then(function () {
        // attachment 수집
        if (seed.view && seed.view.attachments) {
          var selector = webdriver_selector(seed.view.attachments);
          log.info('first attachment selector : ' + JSON.stringify(selector));

          return driver.findElements(selector);
          //return driver.executeScript(selector);
        }
        return undefined;
      })
      .then(function (resp) {
        if (resp && resp.length > 0) {
          var attachment_promises = [];
          var i = 0;
          resp.forEach(function (ele) {
            attachment_promises.push(
              (function () {
                var attachment = {};
                return webdriver.promise
                  .fulfilled()
                  .then(function () {
                    // attachment name 수집
                    if (seed.view.attachments.filter.name && seed.view.attachments.filter.name.type) {
                      var selector = webdriver_selector(seed.view.attachments.filter.name);
                      log.info('second attachment selector : ' + JSON.stringify(selector));
                      i++;
                      return [resp[i - 1]]; //i값으로 첨부파일 인덱스 지정
                      //return ele.findElements(selector);
                    } else {
                      return [resp[0]];
                    }
                    return undefined;
                  })
                  .then(function (resp) {
                    if (resp && resp.length > 0) {
                      if (seed.view.attachments.filter.name.extract.toLowerCase() == 'string') {
                        return seed.view.attachments.filter.name.string;
                      } else if (seed.view.attachments.filter.name.extract.toLowerCase() == 'text') {
                        return resp[0].getText();
                      } else if (seed.view.attachments.filter.name.extract.toLowerCase() == 'attribute_src') {
                        return resp[0].getAttribute('src');
                      } else if (seed.view.attachments.filter.name.extract.toLowerCase() == 'attribute_alt') {
                        return resp[0].getAttribute('alt');
                      } else if (seed.view.attachments.filter.name.extract.toLowerCase() == 'attribute_href') {
                        return resp[0].getAttribute('href');
                      } else if (seed.view.attachments.filter.name.extract.toLowerCase() == 'attribute_onclick') {
                        return resp[0].getAttribute('onclick');
                      } else {
                        log.error(
                          'unknown attachments name extract: ' +
                            JSON.stringify({
                              where: 'seed.view.attachments.filter.name',
                              extract: seed.view.attachments.filter.name.extract,
                            })
                        );
                        setTimeout(error, timeout.global);
                      }
                    }

                    return undefined;
                  })
                  .then(function (resp) {
                    if (resp) {
                      log.info('attachments name: ' + resp); //첨부 파일명
                      attachment.name = resp;
                    }
                    return undefined;
                  })
                  .then(function () {
                    // attachment url 수집

                    if (seed.view.attachments.filter.url && seed.view.attachments.filter.url.type) {
                      var selector = webdriver_selector(seed.view.attachments.filter.url);

                      //return ele.findElements(selector);
                      //return driver.executeScript(selector);
                      return [resp[i - 1]]; //i값으로 첨부파일 인덱스 지정
                    } else {
                      return [resp[0]];
                    }
                    return undefined;
                  })
                  .then(function (resp) {
                    if (resp && resp.length > 0) {
                      if (seed.view.attachments.filter.url.extract.toLowerCase() == 'string') {
                        return seed.view.attachments.filter.url.string;
                      } else if (seed.view.attachments.filter.url.extract.toLowerCase() == 'text') {
                        return resp[0].getText();
                      } else if (seed.view.attachments.filter.url.extract.toLowerCase() == 'attribute_src') {
                        return resp[0].getAttribute('src');
                      } else if (seed.view.attachments.filter.url.extract.toLowerCase() == 'attribute_href') {
                        return resp[0].getAttribute('href');
                      } else if (seed.view.attachments.filter.url.extract.toLowerCase() == 'attribute_onclick') {
                        return resp[0].getAttribute('onclick');
                      } else {
                        log.error(
                          'unknown attachments url extract: ' +
                            JSON.stringify({
                              where: 'seed.view.attachments.filter.url',
                              extract: seed.view.attachments.filter.url.extract,
                            })
                        );
                        setTimeout(error, timeout.global);
                      }
                    }
                    return undefined;
                  })
                  .then(function (resp) {
                    console.log('attach resp : ' + resp);

                    if (resp) {
                      attachment.url = resp;
                    }
                    return undefined;
                  })
                  .then(function () {
                    attachment.name = attachment.name || '';
                    attachment.url = attachment.url || '';
                    return attachment;
                  });
              })()
            );
          });
          return webdriver.promise.all(attachment_promises);
        }
        return undefined;
      })
      .then(function (resp) {
        if (resp) {
          cdoc.attachments = resp;
          log.info('cdoc.attachments : ' + JSON.stringify(cdoc.attachments));
        }
        return undefined;
      })
      .then(function () {
        // 현재 url 획득
        driver
          .getCurrentUrl()
          .then(function (url) {
            var exceptKey = [];
            var bodyObj = {};
            exceptKey.push('seed');
            exceptKey.push('seed_url');
            exceptKey.push('url');
            exceptKey.push('target');

            bodyObj.seed = status.seed;
            bodyObj.url = url;
            bodyObj.seed_url = status.url;
            // var dateCut;
            var pt = /[\r|\n]/g; //저전력 광역 통신망 시장\n출처 :
            // for (var key in seed.view) {

            var savedir = '';
            var attachCheck = false;
            var attachFunc;

            //시드명으로 폴더 생성
            savedir = '/kdata2/web_attach/' + bodyObj.seed;
            if (!fs.existsSync(savedir)) {
              fs.mkdirSync(savedir);
            }

            //파일 다운로드 함수
            async function downloadFile(attachUrl, savedir, attachName, rqMethod, params) {
              const response = await Axios(attachUrl, {
                method: rqMethod,
                params: params,
                responseType: 'stream',
              });

              //reponse 정보로 파일 이름 생성(파일명 존재하지 않을 시 웹에서 추출한 name으로 대체)
              var filename = decodeURI(response.headers['content-disposition'].split('filename=')[1]);

              if (filename === 'null' || filename.length < 0) {
                filename = attachName;
              }

              var regex = /[^\"\;\']+/g;
              filename = filename.match(regex).toString();

              // if (filename === 'null' || filename.length < 0) {
              //   filename = attachName;
              // } else if (site === 'www.kdi.re.kr') {
              //   filename = filename.substring(1, filename.length - 2);
              // } else if (bodyObj.seed === 'web-kistep-publication') {
              //   var regex = /[^\"\;]+/g;
              //   filename = filename.match(regex);
              // }

              const path = Path.resolve(savedir, filename);
              const writer = fs.createWriteStream(path);
              response.data.pipe(writer);

              return new Promise((resolve, reject) => {
                writer.on('finish', resolve);
                writer.on('error', reject);
              });
            }

            for (var key in cdoc) {
              if (exceptKey.indexOf(key) < 0) {
                var tempValue;
                // if (key == 'timestamp') {
                //     tempValue = cdoc[key] || '2050-01-01';
                //     bodyObj.reg_date = moment(tempValue).add(9, 'hours').utc().toISOString();
                //     bodyObj[key] = moment().add(9, 'hours').utc().toISOString();
                //     dateCut = parseInt(moment(tempValue).format('YYYYMMDD'));
                // } else if (key == 'attachments') {
                if (key == 'attachments') {
                  attachCheck = true;
                  tempValue = cdoc[key] || [{}];
                  //url 및 파일명 가공 후 파일 다운로드 함수 promise.all로 처리
                  attachFunc = async function (savedir) {
                    var promise_all = [];
                    promise_all = tempValue.map(async (attach) => {
                      var bUrl = JSON.stringify(attach.url);
                      var attachUrl = bUrl.substring(1, bUrl.length - 1);
                      var bName = JSON.stringify(attach.name);
                      var attachName = bName.substring(1, bName.length - 1);
                      log.info('attachment url : ' + attachUrl);
                      var rqMethod = 'GET';
                      var params = {};
                      //자바스크립트 함수 방식일 경우 url 재가공
                      if (seed.view.attachments.filter.url.value) {
                        var downUrl = seed.view.attachments.filter.url.value;
                        if (seed.view.attachments.filter.url.method) {
                          rqMethod = 'POST';
                        }
                        var paramsArr = seed.view.attachments.filter.url.params;
                        var jsFunc = attachUrl.match(/\(.+?[\)]{2,}|\(.+?\)/g);
                        jsFunc = jsFunc[0].substring(1, jsFunc[0].length - 1);
                        var jsFuncSplit = jsFunc.replace(/ /gi, '').split(',');
                        console.log(jsFuncSplit);
                        for (var i = 0; i < paramsArr.length; i++) {
                          params[paramsArr[i]] = jsFuncSplit[i].substring(1, jsFuncSplit[i].length - 1);
                        }
                        console.log(params);
                        attachUrl = downUrl;
                      }
                      return await downloadFile(attachUrl, savedir, attachName, rqMethod, params);
                    });
                    return Promise.all(promise_all);
                  };
                  bodyObj[key] = JSON.stringify(tempValue);
                } else {
                  tempValue = cdoc[key] || '';
                  bodyObj[key] = tempValue.replace(pt, ' ') || '';
                }
              }
            }

            // bodyObj['@timestamp'] = moment().add(9, 'hours').utc().toISOString();
            bodyObj['@timestamp'] = moment().utc().toISOString();

            // if (Object.keys(bodyObj).indexOf('timestamp') < 0 ) {
            //     bodyObj['@timestamp'] = moment().add(9, 'hours').utc().toISOString();
            //     dateCut = parseInt(moment('2050-01-01').format('YYYYMMDD'));
            // }

            // id 생성 : MD5(SEED).MD5(URL).MD5(TITLE).MD5(CONTENTS);
            // var id = [md5(cdoc.seed), md5(cdoc.url), md5(cdoc.title), md5(cdoc.contents)].join('.');
            // var id = [md5(cdoc.seed), md5(cdoc.timestamp), md5(cdoc.title), md5(cdoc.contents)].join('.');
            // var id = [md5(cdoc.seed), md5(dateCut), md5(cdoc.title), md5(cdoc.contents)].join('.');
            // var id = [md5(bodyObj.seed), md5(bodyObj.url), md5(bodyObj.title), md5(bodyObj.contents)].join('.');

            //필수 파라미터로 구성된 url 추출
            var necessaryParamArr = seed.params;
            var splitText = '';
            if (seed.id === 'itech-report') {
              splitText = '.sub_con|';
            } else {
              splitText = '?';
            }
            var host = bodyObj.url.split(splitText);
            var queryArr = host[1].split('&');
            var newUrl = host[0] + splitText;
            for (var i = 0; i < queryArr.length; i++) {
              for (var l = 0; l < necessaryParamArr.length; l++) {
                if (queryArr[i].split('=')[0] === necessaryParamArr[l]) {
                  newUrl += queryArr[i] + '&';
                }
              }
            }

            var id = [md5(bodyObj.seed), md5(newUrl)].join('.');
            log.info('newUrl : ' + newUrl);
            log.info('id : ' + JSON.stringify(id));

            // 수집된 문서를 ElasticSearch에 저장
            var params = {
              //index: template.contents.index,
              index: seed.index, // $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
              type: 'contents',
              id: id,
              // opType: "create", // check to duplicate document
              body: bodyObj,
            };

            // console.log(JSON.stringify(params, null, 2));
            // return esclient.index(params);

            //엘라스틱 put 요청 함수
            function esRequest() {
              return new Promise(function (resolve, reject) {
                esclient.index(params, function (resp, err) {
                  log.info(JSON.stringify(err.result));
                  if (err.status && err.status === 409) {
                    reject(err);
                    log.info('Aleady exist index document : ' + JSON.stringify(err));
                    setTimeout(stop, timeout.global);
                  } else if (err.status && err.status !== 409) {
                    reject(err);
                    log.error('error in index document : ' + JSON.stringify(err));
                    setTimeout(error, timeout.global);
                  } else if (err.result === 'created') {
                    resolve();
                  } else if (err.result === 'updated') {
		    log.info('Already crawl document ->> crawling stop');
		    setTimeout(stop, timeout.global);
		  } 
                });
              });
            }

            //첨부파일 있으면 첨부파일 다운로드 후 엘라스틱으로 데이터 put 요청
            if (attachCheck) {
              //시드명 아래 id로 하위 폴더 생성 후 해당 폴더로 파일 다운로드
              savedir = '/kdata2/web_attach/' + bodyObj.seed + '/' + id;
              if (!fs.existsSync(savedir)) {
                fs.mkdirSync(savedir);
              }
              return attachFunc(savedir)
                .then(function () {
                  log.info('file download success');
                  return esRequest();
                })
                .catch(function () {
                  log.info('file download error');
                  return esRequest();
                });
            } else {
              log.info('no attach file');
              return esRequest();
            }
          })
          .then(function (resp) {
            //상태정보 업데이트
            console.log('==============');
            console.log('상태정보 resp : ' + resp);
            status.stat.counts++;
            updateHistoryInfo();

            if (seed.lists.target.move_view === undefined || seed.lists.target.move_view === true) {
              driver
                .navigate()
                .back()
                .then(function () {
                  // if( status.stat.counts > 1 ) {
                  //     setTimeout(success, 10);
                  //     return;
                  // }
                  setTimeout(crawl.lists, timeout.move.document_back);
                });
            } else {
              setTimeout(crawl.lists, timeout.move.document_back);
            }
          })
          .catch(function (err) {
            if (err.statusCode == 409) {
              // conflict: stop crawling
              setTimeout(success, timeout.global);
            } else {
              log.error('crawling document error: ' + JSON.stringify(err));
              setTimeout(error, timeout.global);
            }
          });
      });
  },
};

var move = {
  page: function () {
    // 수집된 페이지는 제외
    var page;
    while ((page = status.target.pages.shift())) {
      if (status.visit.pages[page]) {
        continue;
      }
      break;
    }

    if (page) {
      // 이동한 페이지 기록
      status.visit.pages[page] = true;

      // 페이지 이동
      log.debug(
        'move page: ' +
          JSON.stringify({
            page: page,
          })
      );

      if (seed.paging.action.toLowerCase() == 'href') {
        driver
          .navigate()
          .to(page)
          .then(function () {
            setTimeout(build.lists, timeout.move.page);
          });
      } else if (seed.paging.action.toLowerCase() == 'javascript_href') {
        return driver.executeScript(page).then(function () {
          setTimeout(build.lists, timeout.move.page);
        });
      } else if (seed.paging.action.toLowerCase() == 'javascript_onclick') {
        return driver.executeScript(page).then(function () {
          setTimeout(build.lists, timeout.move.page);
        });
      } else {
        log.error(
          'unknown action type: ' +
            JSON.stringify({
              where: 'seed.paging.action',
              action: seed.paging.action,
            })
        );
        setTimeout(error, timeout.global);
      }
    } else if (status.moved_next_paging === true) {
      // 페이징을 이동한 경우 페이지 리스트 수집
      status.moved_next_paging = false;
      setTimeout(build.pages, timeout.global);
    } else {
      // 다음 페이징으로 이동
      status.moved_next_paging = true;
      setTimeout(move.next_paging, timeout.global);
    }
  },
  next_paging: function () {
    if (seed.next_paging) {
      // 다음 페이징 이동
      var selector = webdriver_selector(seed.next_paging);
      driver
        .findElements(selector)
        .then(function (paging) {
          if (seed.next_paging.match && seed.next_paging.match != 'undefined') {
            // 다음 페이징 매칭 조건 검사
            var pages_promise_all = [];
            paging.forEach(function (ele) {
              var selector = webdriver_selector(seed.next_paging.match);
              pages_promise_all.push(
                ele.findElements(selector).then(function (resp) {
                  if (resp.length > 0) {
                    return true;
                  }
                  return false;
                })
              );
            });

            pages_promise_all.push(
              webdriver.promise.all(pages_promise_all).then(function (resp) {
                return resp.every(function (e) {
                  return e;
                });
              })
            );

            // 다음 페이징 정보 반환
            return webdriver.promise.all(pages_promise_all).then(function (resp) {
              var pages = [];
              for (var i in resp) {
                if (paging[i]) {
                  if (resp[i] === true && test_next_page_check) {
                    pages.push(paging[i]);
                    if (seed.test_doc_per_page > 0 && seed.test_max_page_num > 0) {
                      test_next_page_check = false;
                    }
                  }
                }
              }
              return pages;
            });
          } else if (paging && paging.length > 0) {
            if (test_next_page_check) {
              if (seed.test_doc_per_page > 0 && seed.test_max_page_num > 0) {
                test_next_page_check = false;
              }
              return [paging[0]];
            } else {
              return undefined;
            }
          } else {
            return undefined;
          }
        })
        .then(function (resp) {
          if (resp && resp.length > 0) {
            if (seed.next_paging.action.toLowerCase() == 'href') {
              return resp[0].getAttribute('href');
            } else if (seed.next_paging.action.toLowerCase() == 'javascript_href') {
              return resp[0].getAttribute('href');
            } else if (seed.next_paging.action.toLowerCase() == 'javascript_onclick') {
              return resp[0].getAttribute('onclick');
            } else if (seed.next_paging.action.toLowerCase() == 'xpath') {
              return get_xpath(resp[0]);
            } else {
              log.error(
                'unknown action type: ' +
                  JSON.stringify({
                    where: 'seed.next_paging.action',
                    action: seed.next_paging.action,
                  })
              );
              setTimeout(error, timeout.global);
            }
          }
          return undefined;
        })
        .then(function (resp) {
          if (resp) {
            if (status.visit.next_paging[resp]) {
              // 다음 페이징이 이전에 방문을 했다면 종료
              setTimeout(success, timeout.global);
            } else {
              if (!seed.next_paging.ignore_duplicated) {
                // 이동한 다음 페이징 정보 저장
                status.visit.next_paging[resp] = true;
              }

              // 다음 페이징으로 이동
              log.info(
                'move next paging: ' +
                  JSON.stringify({
                    target: resp,
                  })
              );

              if (seed.next_paging.action.toLowerCase() == 'href') {
                driver
                  .navigate()
                  .to(resp)
                  .then(function () {
                    setTimeout(build.lists, timeout.move.next_paging);
                  });
              } else if (seed.next_paging.action.toLowerCase() == 'javascript_href') {
                return driver.executeScript(resp).then(function () {
                  setTimeout(build.lists, timeout.move.next_paging);
                });
              } else if (seed.next_paging.action.toLowerCase() == 'javascript_onclick') {
                return driver.executeScript(resp).then(function () {
                  setTimeout(build.lists, timeout.move.next_paging);
                });
              } else if (seed.next_paging.action.toLowerCase() == 'xpath') {
                var selector = webdriver_selector({
                  type: 'xpath',
                  value: resp,
                });
                driver.findElement(selector).then(function (resp) {
                  if (resp) {
                    driver
                      .actions()
                      .mouseMove(resp)
                      .click()
                      .perform()
                      .then(function (resp) {
                        setTimeout(build.lists, timeout.move.next_paging);
                      })
                      .catch(function (err) {
                        log.error('cannot move to next paging: ' + JSON.stringify(err));
                        setTimeout(error, timeout.global);
                      });
                  } else {
                    log.error('cannot find element to next paging');
                    setTimeout(error, timeout.global);
                  }
                });
              } else {
                log.error(
                  'unknown action type: ' +
                    JSON.stringify({
                      where: 'seed.next_paging.action',
                      action: seed.next_paging.action,
                    })
                );
                setTimeout(error, timeout.global);
              }
            }
          }
          // 시드별 테스트 기능때문에 주석 처리
          // else {
          //     log.error('next paging element is zero');
          //     setTimeout(error, timeout.global);
          // }
        })
        .catch(function (err) {
          log.error('move_next_paging error: ' + JSON.stringify(err));
          setTimeout(error, timeout.global);
        });
    } else {
      setTimeout(success, timeout.global);
    }
  },
};

var error = function () {
  log.error('crawling error');
  setTimeout(stop, timeout.global);
};

var success = function () {
  log.info('crawling end successfully: ' + JSON.stringify(status.stat));

  // 히스토리 업데이트
  updateHistoryInfo();

  driver.quit();
  driver.close();
  driver.dispose();
  process.exit(0);
};
var error = function () {
  log.error('crawling error');
  setTimeout(stop, timeout.global);
};

var stop = function () {
  var paramCon = {
    running: false,
  };
  // crawler-seed 업데이트
  updateSeedInfo(paramCon);
  // 히스토리 업데이트
  updateHistoryInfo();

  if (driver) {
    driver.close().then(function (resp) {
      setTimeout(exit, 1);
    });
  } else {
    setTimeout(exit, 1);
  }
};

var stopped = function () {
  updateHistoryInfo();
  if (driver) {
    driver.close().then(function (resp) {
      setTimeout(exit, 1);
    });
  } else {
    setTimeout(exit, 1);
  }
};

var exit = function () {
  if (log.info !== console.log) {
    // for log flush
    log.info('crawler stopped', function (err, level, msg, meta) {
      driver.quit();
      driver.close();
      driver.dispose();
      process.exit(0);
    });
    return;
  }
  driver.quit();
  process.exit(0);
};

var updateSeedInfo = function (paramContent) {
  // Seed Info 업데이트
  var params = {
    index: template.seed_settings.index,
    type: 'crawler',
    id: seed.id,
    body: {
      doc: paramContent,
    },
  };
  esclient.update(params, function (err, resp) {
    if (err) {
      log.error('Cannot update seed running flag : ' + JSON.stringify(err));
    }
  });
};

var updateHistoryInfo = function () {
  // status.stat.end = moment().add(9, 'hours').utc().toISOString();
  status.stat.end = moment().utc().toISOString();
  // 히스토리 업데이트
  var params = {
    index: template.history.index,
    type: 'history',
    id: status.elasticsearch.id,
    body: {
      doc: {
        end: status.stat.end,
        counts: status.stat.counts,
      },
    },
  };
  log.info('->>> ' + JSON.stringify(params));
  esclient.update(params, function (err, resp) {
    if (err) {
      log.error('cannot index history: ' + JSON.stringify(err));
      setTimeout(error, timeout.global);
    }
    // else {
    //     setTimeout(stop, timeout.global);
    // }
  });
};

var webdriver_selector = function (obj) {
  if (obj.type.toLowerCase() == 'xpath') {
    return webdriver.By.xpath(obj.value);
  } else if (obj.type.toLowerCase() == 'selector') {
    return webdriver.By.css(obj.value);
  }
  return undefined;
};

var get_xpath = function (element) {
  return driver
    .executeScript(
      'getXPath = function(node) {' +
        'if( node.id !== "" ) {' +
        'return "//" + node.tagName.toLowerCase() + "[@id=\'" + node.id + "\']"' +
        '}' +
        'if( node === document.body ) {' +
        'return node.tagName.toLowerCase()' +
        '}' +
        'var nodeCount = 0;' +
        'var childNodes = node.parentNode.childNodes;' +
        'for( var i = 0; i < childNodes.length; i++ ) {' +
        'var currentNode = childNodes[i];' +
        'if( currentNode === node ) {' +
        'return getXPath(node.parentNode) + "/" + node.tagName.toLowerCase() + "[" + (nodeCount+1) + "]"' +
        '}' +
        'if( currentNode.nodeType === 1 && ' +
        'currentNode.tagName.toLowerCase() === node.tagName.toLowerCase() ) {' +
        'nodeCount++' +
        '}' +
        '}' +
        '};' +
        'return getXPath(arguments[0]);',
      element
    )
    .then(function (resp) {
      return resp;
    });
};

module.exports = {
  start: start,
  stop: stopped,
  exit: exit,
};
