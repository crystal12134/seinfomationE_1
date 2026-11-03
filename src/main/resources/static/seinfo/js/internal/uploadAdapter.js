class uploadAdapter {

    constructor(loader, t) {
        this.loader = loader;
        this.t = t;
    }

    upload() {
        return new Promise((resolve, reject) => {

            const reader = this.reader = new FileReader();

            reader.onload = function (){
                resolve({default: reader.result});
            };
            reader.onerror = function (error) {
                reject(error);
            };
            reader.onabort = function () {
                reject();
            };

            this.loader.file.then(file => {
                let size = 1024 * 1024 * 2;

                if(file) {

                    reader.onload = function (e) {
                        const img = new Image();
                        img.onload = () => {

                            const canvas = document.createElement('canvas');
                            const ctx = canvas.getContext('2d');

                            let width = img.width;
                            let height = img.height;

                            let MAX_WIDTH = 500;
                            let MAX_HEIGHT = 500;

                            if (width > height) {
                                if (width > MAX_WIDTH) {
                                    height *= MAX_WIDTH / width;
                                    width = MAX_WIDTH;
                                }
                            } else {
                                if (height > MAX_HEIGHT) {
                                    width = MAX_HEIGHT / height;
                                    height = MAX_HEIGHT;
                                }
                            }

                            const newWidth = Math.floor(width);
                            const newHeight = Math.floor((img.height / img.width) * newWidth);
                            canvas.width = newWidth;
                            canvas.height = newHeight;

                            // 이미지를 Canvas에 맞게 그리기
                            ctx.drawImage(img, 0, 0, newWidth, newHeight);

                            // PNG 이미지를 JEPG로 변환
                            const dataUrl = canvas.toDataURL('image/jpeg');
                            resolve({
                                default: dataUrl
                            });
                        };
                        img.src = reader.result;
                    };
                    reader.onerror = reject;
                    reader.readAsDataURL(file);
                }
            })
        });
    }
    abort() {
        if (this.reader) {
            this.reader.abort();
        }
    }
}