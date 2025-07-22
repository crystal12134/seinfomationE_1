pf.zzzUtil = {};

pf.zzzUtil.isNotEmpty = function(str) {
    if (str === undefined || str === null) {
        return false;
    }

    if (typeof str === 'string') {
        return str !== '' && str !== 'null' && str !== 'undefined';
    }

    if (typeof str === 'object') {
        return Object.keys(str).length > 0;
    }

    return true;
}

pf.zzzUtil.isEmpty = function (str) {
    return !pf.zzzUtil.isNotEmpty(str);
}

pf.zzzUtil.strNvlChange = function (str, nvl) {
    if (pf.zzzUtil.isNotEmpty(str)) {
        return str;
    } else {
        return nvl;
    }
}
pf.zzzUtil.getEncryptParam = function (paramData) {
    let encText = '';
    $.ajax({
        method: 'post',
        url : '/api/getEncryptParam',
        contentType: 'application/json',
        data: JSON.stringify({ paramData : paramData }),
        async: false,
        success : function (rData) {
            encText = rData.encData;
        }
    });
    return encText;
}