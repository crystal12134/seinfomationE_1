pf.seinfoFile = pf.seinfoFile || {};
pf.seinfoFile.tus = pf.seinfoFile.tus || {};

pf.seinfoFile.tus.download = (fileGrpId, fileId, userType) => {
    if(!fileGrpId) {
        pf.seinfoAlert.warning('다운로드할 파일의 그룹정보가 업습니다.');
    }
    if(!fileId) {
        pf.seinfoAlert.warning('다운로드할 파일이 없습니다.');
    }

    pf.seinfo.ajax({userType: userType, url: contextPath+'/tus/mfilePermit', data: { mfileGrpId:fileGrpId, mfileId: fileId} }, (rData) =>{
        if (rData.process_code === '00'){
            location.href = `${contextPath}/private/filedownload?mfileDownId=${rData.data.mfileDownId}`;
        } else {
            pf.seinfoAlert.warning(rData.data.message);
        }
    });
}

pf.seinfoFile.tus.downloadAll = (fileGrpId, userType) => {
    if(!fileGrpId) {
        pf.seinfoAlert.warning('다운로드할 파일이 없습니다.');
    }

    pf.seinfo.ajax({userType: userType, url: contextPath+'/tus/mfilePermit', data: { mfileGrpId:fileGrpId, type: 'zip' }}, (rData) =>{
        if (rData.process_code === '00'){
            location.href = `${contextPath}/private/filedownload?mfileDownId=${rData.data.mfileDownId}`;
        } else {
            pf.seinfoAlert.warning(rData.data.message);
        }
    });
}