pf.seinfo = {};

pf.seinfo.refreshToken = (id) => {
    pf.seinfo.ajax({url: contextPath + '/login/refreshToken', data: {token: pf.token()}, chkToken: 'N'}, (resData) => {
        if (pf.var['tokenSession']) {
            clearInterval(pf.var['tokenSession']);
        }
        pf.seinfo.timeToken(id);
    });
}

pf.seinfo.timeToken = (id) => {
    const tmpToken = pf.token();
    if (!tmpToken || !tmpToken.exp)  return null;

    const formatNumber = num => (num < 10 ? '0' : '') + num;

    const updateTime = () => {
        const tokenTime = new Date(tmpToken.exp * 1000);
        const nowTime = new Date();
        const diff = Math.max(0, tokenTime.getTime() - nowTime.getTime());

        if (diff <= 0) {
            clearInterval(pf.var['tokenSession']);
            $(id).html('00:00:00');
            return null;
        }

        const hour = Math.floor(diff / (60 * 60 * 1000));
        const min = Math.floor((diff / (60 * 1000)) % 60);
        const sec = Math.floor((diff / 1000) % 60);

        const time = `${formatNumber(hour)}:${formatNumber(min)}:${formatNumber(sec)}`;
        $(id).html(time);

        // 자동 로그아웃
        if (hour === 0 && min === 0 && sec === 0) {
            clearInterval(pf.var['tokenSession']);
            let msg = '로그인 세션 시간이 만료되엇습니다. <br>확인 버튼을 누르시면 로그인 페이지로 이동합니다.';
            pf.seinfoAlert.warning(msg, () => {
                pf.seinfo.fnLogOutProcess();
            });
        }
    };

    // 초기 실행
    updateTime();

    // 인터벌 설정 전에 기존 인터벌 제거
    if (pf.var['tokenSession']) {
        clearInterval(pf.var['tokenSession']);
    }

    pf.var['tokenSession'] = setInterval(updateTime, 1000);
};

pf.seinfo.validateToken = () => {
    $('#tokenTime').html('');

    const tmpToken = pf.token();
    if(!tmpToken) {
        return false;
    }

    const tokenTime = new Date(tmpToken.exp * 1000);
    const nowTime = new Date();
    const diff = Math.max(0, tokenTime.getTime() - nowTime.getTime());

    if (diff <= 0) {
        clearInterval(pf.var['tokenSession']);
        pf.seinfo.fnLogOutProcess(false, true);
        return false;
    }

    return true;
};

pf.seinfo.fnLogOut = (returnUrl) => {
    pf.seinfoAlert.confirm('로그아웃 하시겠습니까?', ()=> {
       pf.seinfo.fnLogOutProcess(returnUrl);
    });
}

pf.seinfo.fnLogOutProcess = (returnUrl, refresh) => {
    if(!returnUrl && refresh) {
        returnUrl = window.location.pathname;
    }

    // 세션스토리지 초기화
    Object.keys(sessionStorage).forEach(key => {
        sessionStorage.removeItem(key);
    });

    $.ajax({
        url : contextPath + '/login/logout',
        type: 'POST',
        contentType: 'application/json',
        success : function (rData) {
            if (rData.process_code === '00') {
                if (pf.zzzUtil.isNotEmpty(returnUrl) && returnUrl !== '/admin/ppsHistoryList') {
                    location.href = contextPath + returnUrl;
                } else {
                    location.href = pf.seinfo.fnGetMovePage("divMid110"); // 기본 로그인 페이지
                }
            } else {
                pf.seinfoAlert.warning("[Logout Error] <br>" + rData.process_msg + "<br>" + rData.data);
            }
        },
        error: function (jqXHR) {
            const response = JSON.parse(jqXHR.responseText).message;
            const msg = pf.zzzUtil.strNvlChange(response, jqXHR.responseJSON.process_msg);
            pf.seinfoAlert.warning('[Logout Error2] <br>' + msg);
        }
    });
};

pf.seinfo.ajax = (ajaxconfig, Dcallback) => {
    if(!ajaxconfig) {
        pf.seinfoAlert.warning('param(1) ajax config data required');
        return null;
    } else if(!ajaxconfig.url) {
        pf.seinfoAlert.warning('param(2) ajax config url required');
        return null;
    } else if(!ajaxconfig.data) {
        pf.seinfoAlert.warning('param(3) ajax config data required');
        return null;
    } else if(!Dcallback) {
        pf.seinfoAlert.warning('param(4) ajax callback function required');
        return null;
    }

    const tmpToken = pf.token();
    if(tmpToken && !pf.seinfo.validateToken()) {
        pf.seinfo.fnLogOutProcess(null, true);
        return null;
    }else {
        pf.seinfo.timeToken('#tokenTime');
    }

    let reqData = null;
    let reqUrl = ajaxconfig.url;
    let type = ajaxconfig.method ? ajaxconfig.method : 'POST';
    if (type === 'GET' || type === 'get' || type === 'delete' || type === 'DELETE') {
        reqUrl += '?' + $.param(ajaxconfig.data);
    } else {
        reqData = JSON.stringify(ajaxconfig.data);
    }

    const finalAjaxConfig = {
        url : reqUrl,
        method : type,
        datatype : 'json',
        contentType : 'application/json',
        async : (!ajaxconfig.async) ? false : ajaxconfig.async,
        data : reqData,
        success : (rData) => {
            if(rData.data != null && rData.data.hasOwnProperty('BTN_AUTH_CHECK')) {
                pf.seinfoAlert(rData.process_msg);
            } else {
                if(Dcallback) {
                    pf.seinfo.timeToken('#tokenTime');
                    Dcallback(rData);
                    return null;
                }
            }
        },
        error : (xhr, textStatus, error) => {
            if(ajaxconfig.error) {
                ajaxconfig.error(xhr, textStatus, error);
            } else if(xhr.responseJSON) {
                if(xhr.status === 401 || xhr.status === 403) {
                    if(pf.var !== undefined && pf.var['tokenSession']) {
                        clearInterval(pf.var['tokenSession']);
                    }

                    const tmpToken = pf.token();
                    if (tmpToken === null) {
                        pf.seinfoAlert.warning("토큰이 유효하지 않습니다. 자동 로그아웃 처리 됩니다.");
                    } else {
                        const tokenTime = new Date(tmpToken?.exp * 1000);
                        const nowTime = new Date();
                        const diff = Math.max(0, tokenTime.getTime() - nowTime.getTime());

                        if(diff <= 0){
                            pf.seinfoAlert.warning('[error1] 로그인 세션 시간이 만료 되었습니다. <br> 확인 버튼을 누르시면 로그인 페이지로 이동합니다.', () =>{
                                pf.seinfo.fnLogOutProcess();
                            });
                        }else {
                            pf.seinfoAlert.warning('[error1] 토큰이 유효하지 않습니다. 자동 로그아웃 처리 됩니다.', () => {
                                pf.seinfo.fnLogOutProcess();
                            });
                        }
                    }
                }else {
                    if(Dcallback) {
                        const result = xhr.responseJSON;
                        result['process_code'] = "99";
                        result['data'] = {message:xhr.responseJSON.process_msg};
                        Dcallback(xhr.responseJSON);
                    } else {
                        pf.seinfoAlert.warning(xhr.responseJSON.process_msg);
                    }
                }
            }else {
                pf.seinfoAlert.warning('시스템 오류가 발생하였습니다. 잠시 후 이용하세요');
            }
        }
    };

    finalAjaxConfig.beforeSend = (xhr) => {
        if(ajaxconfig.token) {
            xhr.setRequestHeader('X-AUTH-TOKEN', ajaxconfig.token);
        }
    }

    $.ajax(finalAjaxConfig);
}

pf.seinfo.fnMenuClick = (cMenuId) => {
    const openMenu = pf.seinfo.fnGetMovePage(cMenuId, 'MENU');

    if(pf.zzzUtil.isEmpty(openMenu)) {
        pf.seinfoAlert.warning('[menu error] <br> 화면을 호출 할 수 없습니다.');
        return null;
    }

    if (openMenu === 'N') {
        const tmpToken = pf.token();
        if(!tmpToken) {
            location.href = pf.seinfo.fnGetMovePage("divMid110") + `?prevPage=${cMenuId}`;
            return null;
        }
    }

    const rtnToken = pf.token();
    if(!rtnToken && openMenu === 'Y') {
        location.href = pf.seinfo.fnGetMovePage(cMenuId);
    }else {
        pf.seinfo.fnMenuClickAfter(cMenuId, openMenu);
    }
}

pf.seinfo.fnMenuClickAfter = (cMenuId, openMenu) => {
    const movePage = pf.seinfo.fnGetMovePage(cMenuId);

    if (openMenu === 'N') {
        const tmpToken = pf.token();
        if (tmpToken === null) {
            pf.seinfoAlert.warning('권한이 불충분합니다.');
            return null;
        } else {
            location.href = movePage;
            return null;
        }
    }else {
        location.href = movePage;
        return null;
    }
}

pf.seinfo.fnGetMovePage = (cMenuId, code) => {
    let openMenu = '';
    let movePage = '';

    if(cMenuId === 'divMid410') { // 제안서 작성
        movePage = "/request/requestRegForm";
        openMenu = "N";
    } else if(cMenuId === 'divMid420') { // 제안 이력조회
        movePage = "/request/requestList";
        openMenu = "N";
    } else if(cMenuId === 'divMid530') { // 자유게시판
        movePage = "/board/freeBoardList";
        openMenu = "N";
    } else if(cMenuId === 'divMid610') { // 1:1문의
        movePage = "/board/qnaList";
        openMenu = "N";
    } else if(cMenuId === 'divMid620') { // 회원정보수정
        movePage = "/member/myInfo";
        openMenu = "N";
    } else if(cMenuId === 'divMid630') { // 업무일지등록
        movePage = "/member/scheduler";
        openMenu = "N";
    } else if(cMenuId === 'divMid640') { // 탈퇴하기
        movePage = "/member/dropInfo";
        openMenu = "N";
    } else if(cMenuId === 'divMid700') { // 관리자페이지
        movePage = "/admin/ppsHistoryList";
        openMenu = "N";
    } else if(cMenuId === "divTop000") { // 메인페이지
        movePage = "/";
        openMenu = "Y"
    } else if(cMenuId === 'divMid110') { // 로그인 페이지
        movePage = '/login/login';
        openMenu = "Y";
    } else if(cMenuId === "divMid210") { // 회원가입
        movePage = "/anyone/member/joinMember";
        openMenu = "Y";
    } else if(cMenuId === 'divMid010') { // 개인정보처리방침
        movePage = "/anyone/docPolicy";
        openMenu = "Y";
    } else if(cMenuId === 'divMid020') { // 이용약관
        movePage = "/anyone/docAgree";
        openMenu = "Y";
    } else if(cMenuId === 'divMid310') { // 회사소개
        movePage = "/anyone/companyInfo";
        openMenu = "Y";
    } else if(cMenuId === 'divMid510') { // 공지사항
        movePage = "/anyone/noticeList";
        openMenu = "Y";
    } else if(cMenuId === 'divMid520') { // FAQ
        movePage = "/board/faqList";
        openMenu = "Y";
    }

    // 결과값 리턴
    if (code === 'MENU') {
        return openMenu;
    } else {
      return contextPath + movePage;
    }
}