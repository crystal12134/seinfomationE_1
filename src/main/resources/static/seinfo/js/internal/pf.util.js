const getCookie = (name) => {
    try {
        const cookies = document.cookie.split(';');
        for (let cookie of cookies) {
            const [cookieName, cookieValue] = cookie.split('=').map(c => c.trim());
            if (cookieName === name) {
                return decodeURIComponent(cookieValue);
            }
        }
        return null;
    } catch (error) {
        console.error('Error reading cookie:', error);
        return null;
    }
};

pf = pf || {};
pf.var = pf.var || {};

pf.token = () => {
    if (pf.sessionCheckedAt && Date.now() - pf.sessionCheckedAt < 3000) return pf.sessionInfo || null;
    let info = null;
    $.ajax({url: contextPath + '/login/sessionInfo', method: 'GET', async: false, cache: false,
        success: function (data) { if (data && data.exp) info = data; }
    });
    pf.sessionInfo = info; pf.sessionCheckedAt = Date.now(); return info;
};

// 공통코드 불러오기
pf.code= pf.code || {};
pf.code.setList = {};
pf.code.getList = (codeParentId) => {
    $.ajax({
        method: 'POST',
        url: '/api/getCodeList',
        contentType: 'application/json',
        data: JSON.stringify({codeParentId: codeParentId}),
        async: false,
        success : function (response) {
            let arr = [];
            for(let tmp of response.codeList) {
                arr.push({name : tmp.codeNm, value: tmp.codeValue });
            }
            pf.code.setList[codeParentId] = arr;
        },
        error: function (jqXHR) {
           const response = JSON.parse(jqXHR.responseText).message;
           console.error(response);
       }
    });
}

// uuid 생성
pf.code.uuid = () => {
    const bytes = new Uint8Array(16);
    crypto.getRandomValues(bytes);

    // UUID version 4 형식 지정 (rfc4122 규격 준수)
    bytes[6] = (bytes[6] & 0x0f) | 0x40; // version 4
    bytes[8] = (bytes[8] & 0x3f) | 0x80; // variant (10xxxxxx)

    const hex = [...bytes].map(b => b.toString(16).padStart(2, '0'));

    return [
        hex.slice(0, 4).join(''),
        hex.slice(4, 6).join(''),
        hex.slice(6, 8).join(''),
        hex.slice(8, 10).join(''),
        hex.slice(10, 16).join('')
    ].join('-');
}

//포멧
pf.format = {};
pf.format = {
    date: (date, format) => {
        return pf.date.getFormat(date, format);
    }
}

// 날짜 생성
pf.date = {
    getFullDate : (data) => {
        if(!data) return '';
        let tmp = new moment(data);
        return tmp.tz('Asia/Seoul').format('YY-MM-DD HH:mm');
    },
    getFullDateWithSecond: (data) => {
        if(!data) return '';
        let tmp = new moment(data);
        return tmp.tz('Asia/Seoul').format('YY-MM-DD HH:mm ss');
    },
    getDate : (data) => {
        if(!data) return '';
        let tmp = new moment(data);
        return tmp.tz('Asia/Seoul').format('YY-MM-DD');
    },
    getHour : (data) => {
        if(!data) return '';
        let tmp = new moment(data);
        return tmp.tz('Asia/Seoul').format('HH');
    },
    getHourMinuteSecond : (data) => {
        if(!data) return '';
        let tmp = new moment(data);
        return tmp.tz('Asia/Seoul').format('HH:mm:ss');
    },
    getMinute : (data) => {
        if(!data) return '';
        let tmp = new moment(data);
        return tmp.tz('Asia/Seoul').format('mm');
    },
    getMinuteSecond : (data) => {
        if(!data) return '';
        let tmp = new moment(data);
        return tmp.tz('Asia/Seoul').format('mm:ss');
    },
    getTime : (data) => {
        if(!data) return '';
        let tmp = new moment(data);
        return tmp.tz('Asia/Seoul').format('HH:mm');
    },
    getFormat : (data, dateFormat) => {
        if(!data) return '';
        let tmp = new moment(data);
        return tmp.tz('Asia/Seoul').format(dateFormat);
    }
}