pf.seinfoComUtil = {};

/**
 * 한글, 영문, 숫자 허용  :
 *  한글, 영문, 숫자, 공백 허용 :
 */
pf.seinfoComUtil.autoVaildator = (str, maxByteLength) => {
    const isStrict = (str === '#userNm' || str === '#ceoNm' || str === '#usrNm');
    // 1. 즉시 글자수 자르기
    $(document).on('input', str, function () {
       const val = $(this).val();
       const trimmed = val.substring(0, maxByteLength);
       if (val !== trimmed) {
          $(this).val(trimmed);
        }
    });

    // 2. focusout시 특수문자 제거
    if(str){
        $(document).on('focusout', str, function () {
            const val = $(this).val();
            const cleaned = val.replace(isStrict? /[^가-힣a-zA-Z0-9]/g : /[^가-힣a-zA-Z0-9\s]/g, '');
            if (val !== cleaned) {
                $(this).val(cleaned);
            }
        });
    }
}

/**
 * 사업자번호 자동생성
 */
pf.seinfoComUtil.formatSetCompanyNo = (str) => {
    $(document).on('input', str, function () {
       let input = $(this).val().replace(/[^0-9]/g, ''); // 숫자만 남기기

       if (input.length > 10) {
           input = input.substring(0, 10); // 10자리 초과 입력 방지
       }

       if (input.length > 3 && input.length <= 5){
           input = input.replace(/(\d{3})(\d+)/, '$1-$2'); // 첫 번째 '-' 추가
       }else if (input.length > 5) {
           input = input.replace(/(\d{3})(\d{2})(\d+)/, '$1-$2-$3'); // 두번째 '-' 추가
       }

       $(this).val(input);
    });
}

/**
 * 연락처 자동생성
 */
pf.seinfoComUtil.formatSetPhoneNo = (str) => {
    $(document).on('input', str, function () {
        let input = $(this).val().replace(/[^0-9]/g, ''); // 숫자만 남기기

        // 가운데번호 자릿수 체크 - 무조건 3 혹은 4자리
        if (input.startsWith("02")){
            if (input.length > 2 && input.length <= 5){
                input = input.replace(/(\d{2})(\d+)/,'$1-$2');
            } else if(input.length > 5 && input.length <= 9) {
                input = input.replace(/(\d{2})(\d{3})(\d+)/,'$1-$2-$3');
            } else if(input.length > 9) {
                input = input.replace(/(\d{2})(\d{4})(\d{4})/,'$1-$2-$3');
            }

            if (input.length > 12) {
                input = input.substring(0, 12);
            }
        }else {
            if (input.length > 3 && input.length <= 6){
                input = input.replace(/(\d{3})(\d+)/,'$1-$2');
            } else if(input.length > 6 && input.length <= 10) {
                input = input.replace(/(\d{3})(\d{3})(\d+)/,'$1-$2-$3');
            } else if(input.length > 10) {
                input = input.replace(/(\d{3})(\d{4})(\d{4})/,'$1-$2-$3');
            }

            if (input.length > 13) {
                input = input.substring(0, 13);
            }
        }

        $(this).val(input);
    });
}

/**
 * 날짜 자동생성 (date)
 */
pf.seinfoComUtil.formatSetDate = (str) => {
    $(document).on('focusout', str, function () {
        let input = $(this).val().replace(/[^0-9]/g, ''); // 숫자만 남기기
        let numStr = input.toString();
        let year = numStr.substring(0, 4).padStart(4, '190');
        let month = numStr.substring(4, 6).padStart(2, '01');
        let day = numStr.substring(6, 8).padStart(2, '01');

        input = `${year}-${month}-${day}`;

        if(numStr.length === 0) {
            input = '';
        }
        $(this).val(input);
    });
}

/**
 * 비밀번호를 체크
 */
pf.seinfoComUtil.validatePassword = (pwd, pwdConf, msg1, msg2, msg3, msg4, obj, objConf) => {
    let hasUpperCase = /[A-Za-z]/.test(pwd);
    let hasNumber = /\d/.test(pwd);
    let hasSpecialChar = /[!@#$%^&*(),.?":{}|<>/]/.test(pwd);

    let combinationCount = [hasUpperCase, hasNumber, hasSpecialChar].filter(Boolean).length;

    if(!pwd) {
        pf.seinfoAlert.focus(0, msg1, obj);
        return false;
    }

    if((combinationCount === 3 && pwd.length < 8) ||
        (combinationCount === 2 && pwd.length < 10) ||
        (combinationCount < 2)) {
        pf.seinfoAlert.focus(0, msg2, obj);
        return false;
    }

    if(pwd.length > 20) {
        pf.seinfoAlert.focus(0, msg3, obj);
        return false;
    }

    if(pwd !== pwdConf) {
        pf.seinfoAlert.focus(0, msg4, objConf);
    }

    return true;
}

/**
 * 이메일 체크
 */
pf.seinfoComUtil.validateEmail = (str, msg1, msg2, obj) => {
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    if(!str) {
        pf.seinfoAlert.focus(0, msg1, obj);
        return false;
    }
    if (!emailRegex.test(str)) {
        pf.seinfoAlert.focus(0, msg2, obj);
        return false;
    }
    return true;
}

/**
 * 휴대폰 체크
 */
pf.seinfoComUtil.validatePhoneNo = (str, msg1, msg2, msg3, obj) => {
    const phonePattern = /^(02-\d{3,4}-\d{4})|0\d{2}-\d{3,4}-\d{4}$/;
    const invalidPhone = /^(02-d{3,4}-0000)|0\d{2}-\d{3,4}-0000}$/;

    if(!str) {
        pf.seinfoAlert.focus(0, msg1, obj);
        return false;
    }
    if(!phonePattern.test(str)) {
        pf.seinfoAlert.focus(0, msg2, obj);
        return false;
    }
    if(invalidPhone.test(str)) {
        pf.seinfoAlert.focus(0, msg3, obj);
        return false;
    }
    return true;
}

/**
 * 이름 체크
 */
pf.seinfoComUtil.validateName = (str, msg1, msg2, msg3, msg4, obj, length) => {
    let specialCharPattern = /[^가-힣A-Za-z0-9\s]/g; //한글, 영문 숫자, 공백 허용
    if(obj === '#userNm' || obj === '#ceoNm' || obj === "#usrNm") {
        specialCharPattern = /[^가-힣A-Za-z0-9]/g; //한글, 영문, 숫자 허용
    }

    const specialCharMatches = str.match(specialCharPattern) || [];

    if(!str) {
        pf.seinfoAlert.focus(0, msg1, obj);
        return false;
    }

    // 최소 글자수 체크
    if(str.length < 2) {
        pf.seinfoAlert.focus(0, msg2, obj);
        return false;
    }

    if(specialCharMatches.length > 0) {
        pf.seinfoAlert.focus(0, msg3, obj);
        return false;
    }

    if(length && str.length > length) {
        pf.seinfoAlert.focus(0, msg4, obj);
        return false;
    }
    return true;
}

/**
 * 주소 체크
 */
pf.seinfoComUtil.validateAddress = (str, msg1, msg2, obj, length) => {
    if(!str) {
        pf.seinfoAlert.focus(0, msg1, obj);
        return false;
    }
    return true;
}

/**
 * 날짜 체크 (date)
 */
pf.seinfoComUtil.validateDate = (dateString, msg1, msg2, msg3, obj) => {
    if(!dateString) {
        pf.seinfoAlert.focus(0, msg1, obj);
        return false;
    }

    const input = dateString.replace(/[^0-9]/g, '');
    if((input.length <=0 || pf.zzzUtil.isEmpty(input)) && dateString !== "--") {
        pf.seinfoAlert.focus(0, msg1, obj);
    }

    const regex = /^\d{4}-\d{2}-\d{2}$/;
    if(!regex.test(dateString)) {
        pf.seinfoAlert.focus(0, msg2, obj);
        return false;
    }

    const date = new Date(dateString);
    const [year, month, day] = dateString.split('-');
    if(date.getFullYear() !== parseInt(year) || date.getMonth() + 1 !== parseInt(month) || date.getDate() !== parseInt(day)) {
        pf.seinfoAlert.focus(0, msg3, obj);
        return false;
    }
    if(parseInt(year) < 1900) {
        pf.seinfoAlert.focus(0, msg3, obj);
        return false;
    }
    return true;
}

/**
 * 달력 (calendar)
 */
pf.seinfoComUtil.datePicker = (obj) => {
    $.datepicker.regional['ko'] = {
        closeText: "닫기",
        prevText: "이전",
        nextText: "다음",
        currentText: "오늘",
        monthNames: ["1월", "2월", "3월", "4월", "5월", "6월",
            "7월", "8월", "9월", "10월", "11월", "12월"],
        monthNamesShort: ["1월", "2월", "3월", "4월", "5월", "6월",
            "7월", "8월", "9월", "10월", "11월", "12월"],
        dayNames: ["일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일"],
        dayNamesShort: ["일", "월", "화", "수", "목", "금", "토"],
        dayNamesMin: ["일", "월", "화", "수", "목", "금", "토"],
        weekHeader: "주",
        dateFormat: "yy-mm-dd",
        firstDay: 0,
        isRTL: false,
        showMonthAfterYear: true,
        yearSuffix: "년"
    }
    $.datepicker.setDefaults($.datepicker.regional['ko']);
    obj.datepicker({
        dateFormat: 'yy-mm-dd',
        changeMonth: true,
        changeYear: true,
        showAnim: "fadeIn"
    })
}

/**
 * 공통코드 변환
 */
pf.seinfoComUtil.fnTransCommonCode = (codeList, responseData, responseDataKey, newColumnName) => {
    // responseData가 배열인 경우
    if (Array.isArray(responseData)) {
        return responseData.map(obj => {
            let newObj = {...obj};

            let matched = codeList.find(code => code.value === obj[responseDataKey]);
            if (matched) {
                newObj[newColumnName] = matched.name;
            }
            return newObj;
        })
    }else if (typeof responseData === 'object' && responseData !== null) {
        let newObj = {...responseData};

        let matched = codeList.find(codeObj => codeObj.value === responseData[responseDataKey]);
        if (matched) {
            newObj[newColumnName] = matched.name;
        }
        return newObj;
    }
}

/**
 * 다중 <BR>
 */
pf.seinfoComUtil.insertBr = (text, maxSize) => {
    const cleanedText = text.replace(/<br\s*\/?>/gi, '');


    const regex = new RegExp(`.{1,${maxSize}}`, 'g');
    return cleanedText.match(regex).join('<br/>');
}

/**
 * 긴 제목 처리
 */
pf.seinfoComUtil.sliceText = (text, maxSize) => {
    if(text.length > maxSize) {
        return text.slice(0, maxSize) + '...';
    }
    return text;
}

/**
 *  html 태그 치환 : alert용
 */
pf.seinfoComUtil.funcUnEscape = (html) => {
    if(pf.zzzUtil.isEmpty(html)) return html;
    html = html.replaceAll("& le;", '<=');
    html = html.replaceAll("& ge;", '>=');
    html = html.replaceAll("& lt;", '<');
    html = html.replaceAll("& gt;", '>');
    html = html.replaceAll("& ne;", '=/');
    html = html.replaceAll("& equals;", '=');

    return html;
}

/**
 *  html 태그 치환 : 화면용
 */
pf.seinfoComUtil.unEscape = (htmlStr) => {
    if(pf.zzzUtil.isEmpty(htmlStr)) return '';

    htmlStr = htmlStr.replaceAll("/&lt;/g", '<');
    htmlStr = htmlStr.replaceAll("/&gt;/g", '>');
    htmlStr = htmlStr.replaceAll("/&le;/g", '<=');
    htmlStr = htmlStr.replaceAll("/&ge;/g", '>=');
    htmlStr = htmlStr.replaceAll("/&equals;/g", '=');
    htmlStr = htmlStr.replaceAll("/&ne;/g", '!=');
    htmlStr = htmlStr.replaceAll("/&#89;/g", "'");
    htmlStr = htmlStr.replaceAll("/&quot;/g", '"');
    htmlStr = htmlStr.replaceAll("/&amp;/g", '&');

    return htmlStr;
}

/**
 *  신규 제안서 작성 : 기본문구
 */
pf.seinfoComUtil.initDetailText =() => {
    const defaultText = `
        예시)
          <br>1. 제안 개요 
          <br>&nbsp;&nbsp; · 프로젝트 명 :
          <br>&nbsp;&nbsp; · 제안 기업 : (회사 명칭 입력)
          <br>&nbsp;&nbsp; · 목적 :
          <br>&nbsp;&nbsp; · 제안 내용 :
          <br>
          <br>2. 관련 수행 경력
          <br>&nbsp;&nbsp; · 프로젝트명 :
          <br>&nbsp;&nbsp; · 수행기관(업체) :
          <br>&nbsp;&nbsp; · 수행기간 : (연/월/일)
          <br>&nbsp;&nbsp; · 성과 :
          <br>
          <br>3. 보유 특허
          <br>&nbsp;&nbsp; · 특허명 :
          <br>&nbsp;&nbsp; · 출원번호 :
          <br>&nbsp;&nbsp; · 등록일 : (연/월/일)
          <br>&nbsp;&nbsp; · 설명 :
          <br>
          <br>4. 관련 수상 내역
          <br>&nbsp;&nbsp; · 수상명 :
          <br>&nbsp;&nbsp; · 수여기관 :
          <br>&nbsp;&nbsp; · 수여일 : (연/월/일)
          <br>
          <br>5. 기타
        `;
    return defaultText;
}

/**
 * 페이지이동 제어 : 뒤로가기, 앞으로가기로 접근금지
 */
pf.seinfoComUtil.doNotMovePage = () => {
    window.onpageshow = function(event) {
        if (event.persisted) {
            location.href = location.href;
            return false;
        }
    };
    return true;
}

