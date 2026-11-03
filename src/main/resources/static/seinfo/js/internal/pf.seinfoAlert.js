pf.seinfoAlert = {
    running: false
};

pf.seinfoAlert = (msg, Dcallback) => {
    if(!pf.seinfoAlert.running) {
        pf.seinfoAlert.running = true;

        top.bootbox.alert({
            title : '알림',
            message:
                `<div class="msg-wrap">
                    <div class="msg-area"></div>             
                 </div>`,
            backdrop: 'static',
            centerVertical: true,
            buttons: {
                ok : {
                    label: '확인',
                    className: 'primary-md-btn',
                    id: 'confirmBtn'
                },
            },
            callback: function () {
                pf.seinfoAlert.running = false;
                if(Dcallback) Dcallback();
            },
            onEscape: function () {
                pf.seinfoAlert.running = false;
                if(Dcallback) Dcallback();
            }
        });

        const replaceMsg = pf.seinfoComUtil.funcUnEscape(msg);
        $(".msg-area").html(replaceMsg);
        $(".modal-header .modal-title").addClass('title-information')
        $(".modal-footer button").removeClass('btn');
        $("button").removeClass('bootbox-cancel');
        $("button").removeClass('bootbox-accept');
    }

    // 확인버튼에 focus 적용
    setTimeout(() => {
        $(".confirmBtn").focus();}, 500
    );
};

pf.seinfoAlert.warning = (msg, Dcallback, className) => {
    if(!pf.seinfoAlert.running) {
        pf.seinfoAlert.running = true;

        top.bootbox.alert({
            title : '경고',
            message:
                `<div class="msg-wrap">
                    <div class="msg-area"></div>             
                 </div>`,
            backdrop: 'static',
            centerVertical: true,
            buttons: {
                ok : {
                    label: '확인',
                    className: 'primary-md-btn',
                    id: 'confirmBtn'
                },
            },
            callback: function () {
                pf.seinfoAlert.running = false;
                if(Dcallback) Dcallback();
            },
            onEscape: function () {
                pf.seinfoAlert.running = false;
                if(Dcallback) Dcallback();
            }
        });

        if (className) {
            $('.modal-dialog').addClass(className);
        }

        const replaceMsg = pf.seinfoComUtil.funcUnEscape(msg);
        $(".msg-area").html(replaceMsg);
        $(".modal-header .modal-title").addClass('title-warning');
        $(".modal-footer button").removeClass('btn');
        $("button").removeClass('bootbox-cancel');
        $("button").removeClass('bootbox-accept');
    }

    // 확인버튼에 focus 적용
    setTimeout(() => {
        $(".confirmBtn").focus();}, 500
    );
};

pf.seinfoAlert.success = (msg, Dcallback) => {
    if(!pf.seinfoAlert.running) {
        pf.seinfoAlert.running = true;

        top.bootbox.alert({
            title : '성공',
            message:
                `<div class="msg-wrap">
                    <div class="msg-area"></div>             
                 </div>`,
            backdrop: 'static',
            centerVertical: true,
            buttons: {
                ok : {
                    label: '확인',
                    className: 'primary-md-btn',
                    id: 'confirmBtn'
                },
            },
            callback: function () {
                pf.seinfoAlert.running = false;
                if(Dcallback) Dcallback();
            },
            onEscape: function () {
                pf.seinfoAlert.running = false;
                if(Dcallback) Dcallback();
            }
        });

        const replaceMsg = pf.seinfoComUtil.funcUnEscape(msg);
        $(".msg-area").html(replaceMsg);
        $(".modal-header .modal-title").addClass('title-success')
        $(".modal-footer button").removeClass('btn');
        $("button").removeClass('bootbox-cancel');
        $("button").removeClass('bootbox-accept');
    }

    // 확인버튼에 focus 적용
    setTimeout(() => {
        $(".confirmBtn").focus();}, 500
    );
};

pf.seinfoAlert.focus = (seq, msg, Dcallback) => {
    const koArr = ['알림', '경고', '성공'];
    const cssArr = ['title-information', 'title-warning', 'title-success'];
    const koTitle = koArr[seq];
    const cssTitle = cssArr[seq];

    if(!pf.seinfoAlert.running) {
        pf.seinfoAlert.running = true;

        top.bootbox.alert({
            title : `${koTitle}`,
            message:
                `<div class="msg-wrap">
                    <div class="msg-area"></div>             
                 </div>`,
            backdrop: 'static',
            centerVertical: true,
            buttons: {
                ok : {
                    label: '확인',
                    className: 'primary-md-btn',
                    id: 'confirmBtn'
                },
            },
            callback: function () {
                pf.seinfoAlert.running = false;
                if(Dcallback) {
                    setTimeout(() => {Dcallback.focus();}, 50);
                }
            },
            onEscape: function () {
                pf.seinfoAlert.running = false;
                if(Dcallback) {
                    setTimeout(() => {Dcallback.focus();}, 50);
                }
            }
        });

        const replaceMsg = pf.seinfoComUtil.funcUnEscape(msg);
        $(".msg-area").html(replaceMsg);
        $(".modal-header .modal-title").addClass(cssTitle);
        $(".modal-footer button").removeClass('btn');
        $("button").removeClass('bootbox-cancel');
        $("button").removeClass('bootbox-accept');
    }

    // 확인버튼에 focus 적용
    setTimeout(() => {
        $(".confirmBtn").focus();}, 500
    );
};

pf.seinfoAlert.confirm = (confirmText, Dcallback, isText, Ccallback) => {
    confirmText = (confirmText? confirmText : "확인하시겠습니까?");
    if(!Dcallback) {
        Dcallback = (data) => {
          if(data) pf.seinfoAlert.success("OK");
        };
    }

    top.bootbox.confirm({
        title : '확인',
        message:
            `<div class="msg-wrap">
                <div class="msg-area"></div>             
             </div>`,
        centerVertical: true,
        buttons: {
            cancel:
                {
                    className: 'outline-md-btn',
                    label: '취소'
                },
            confirm:
                {
                    className: 'primary-md-btn',
                    label : '확인',
                    id: 'confirmBtn',
                }
        },
        callback: function (data) {
            if(data) {
                Dcallback();
            }else {
                if(Ccallback) Ccallback();
            }
        }
    });

    const replaceMsg = pf.seinfoComUtil.funcUnEscape(confirmText);
    $(".msg-area").html(replaceMsg);
    $(".modal-header .modal-title").addClass('title-confirm');
    $(".modal-footer button").removeClass('btn');
    $("button").removeClass('bootbox-cancel');
    $("button").removeClass('bootbox-accept');

    // 확인버튼에 focus 적용
    setTimeout(() => {
        $(".confirmBtn").focus();}, 500
    );
};

pf.seinfoAlert.message = (confirmText, Ccallback) => {
    confirmText = (confirmText? confirmText : "메세지를 작성해주세요.");

    top.bootbox.confirm({
        title : '제출',
        message:
            `<div class="msg-wrap">
                <div class="msg-area"></div>
                <div class="input-area mt-1">     
                    <input id="textMessage" type="text"/>   
                </div>     
             </div>`,
        centerVertical: true,
        buttons: {
            cancel:
                {
                    className: 'outline-md-btn',
                    label: '취소'
                },
            confirm:
                {
                    className: 'primary-md-btn',
                    label : '확인',
                    id: 'confirmBtn',
                }
        },
        callback: function (data) {
            if(data) {
                return $("#textMessage").val();
            }else {
                if(Ccallback) Ccallback();
            }
        }
    });

    const replaceMsg = pf.seinfoComUtil.funcUnEscape(confirmText);
    $(".msg-area").html(replaceMsg);
    $("#textMessage").val("");
    $(".modal-header .modal-title").addClass('title-confirm');
    $(".modal-footer button").removeClass('btn');
    $("button").removeClass('bootbox-cancel');
    $("button").removeClass('bootbox-accept');

    // 확인버튼에 focus 적용
    setTimeout(() => {
        $("#textMessage").focus();}, 500
    );
};