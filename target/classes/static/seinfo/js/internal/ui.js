$(document).ready(function () {

    // 모달 공통
    modalHandler();
    
    // 메뉴 드롭다운
    gnbMenu();

	//200자
	maxChars();

    observeModalChanges();

    //모달 스크롤 감지
    modalScrollHandler();

    fileScroll();

    $("input").attr("autocomplete", "off");

    // 패밀리사이트 셀렉트
    $(".family-site-select").on("click", function () {
        const $button = $(this);
        const $img = $button.find("img");
        const $familySiteList = $(".family-site-list");

        $familySiteList.slideToggle(300);

        if ($img.attr("alt") === "펼치기") {
            $img.attr("src", "/static/seinfo/img/icon-familysite-minus.svg").attr("alt", "접기");
        } else {
            $img.attr("src", "/static/seinfo/img/icon-familysite-plus.svg").attr("alt", "펼치기");
        }
    });

    // 메인 슬라이드
    let mainSwiper = new Swiper(".mainSwiper", {
        pagination: {
            el: ".swiper-pagination",
            type: "fraction",
        },
        navigation: {
            nextEl: ".swiper-button-next",
            prevEl: ".swiper-button-prev",
        },
        loop: true,
        autoplay: {
            delay: 5000,
            disableOnInteraction: false,
        },
        effect: "slide",
        speed: 800
    });

    // 공지사항 슬라이드
    let noticeSwiper = new Swiper(".noticeSwiper", {
        direction: 'vertical',
        loop: true,
        autoplay: {
            delay: 5000,
            disableOnInteraction: false,
        },
        effect: "slide",
        speed: 800
    });

    $('#fullpage').fullpage({
        autoScrolling: true, 
        slidesNavigation: true, 
        navigationPosition: 'right',
        scrollingSpeed: 800, 
        easingcss3: 'ease', 
        easing: 'easeInOutCubic',
        anchors: ["page1", "page2"], 
        // 섹션 로딩 후 이벤트
        afterLoad: function (anchorLink, index) {
          if (index === 1) {
            $('#fp-nav').addClass('ver1');
            $('#fp-nav ul li .fp-nav01').addClass('active');
          } 
        },
        // 섹션 이동 시 이벤트
        onLeave: function (index, nextIndex, direction) {
          if (index === 1 && direction === 'down') {
            $('#fp-nav').removeClass('ver1').addClass('ver2');
            $('#fp-nav ul li .fp-nav01').removeClass('active');
            $('#fp-nav ul li .fp-nav02').addClass('active');
          } else if (nextIndex === 1 && direction === 'up') {
            $('#fp-nav').removeClass('ver2').addClass('ver1');
            $('#fp-nav ul li .fp-nav02').removeClass('active');
            $('#fp-nav ul li .fp-nav01').addClass('active');
          }

        },
    });
    mainSwiper.on('init', function () {
        $('.swiper-bullet li').removeClass('active');
        $('.swiper-bullet li:nth-child(1)').addClass('active'); 
    });
    mainSwiper.emit('init');
    // 메인 슬라이드 페이지네이션
    mainSwiper.on('slideChange', function () {
        var activeIndex = mainSwiper.realIndex;
        $('.swiper-bullet li').removeClass('active');
        if (activeIndex === 0) {
            $('.swiper-bullet li:nth-child(1)').addClass('active');
        } else if (activeIndex === 1) {
            $('.swiper-bullet li:nth-child(2)').addClass('active');
        } else if (activeIndex === 2) {
            $('.swiper-bullet li:nth-child(3)').addClass('active');
        }
    });

    $('.swiper-bullet li').on('click', function () {
        var bulletIndex = $(this).index();
        mainSwiper.slideToLoop(bulletIndex);
    });

    $('.swiper-button-stop').click(function () {
        mainSwiper.autoplay.stop();
        $(this).hide();
        $('.swiper-button-play').show();
    });

    $('.swiper-button-play').click(function () {
        mainSwiper.autoplay.start();
        $(this).hide();
        $('.swiper-button-stop').show();
    });
});

function modalHandler() {
    $(".open-modal-btn").on("click", function () {
        const modalId = $(this).data("modal");
        $(modalId).show().css("display", "block");
        $("body").css("overflow", "hidden");
    });

    $(".close-modal-btn, .comm-close-modal-btn").on("click", function () {
        const $modal = $(this).closest(".modal");
        $modal.hide();
        $("body").css("overflow", "");
    });

    $(document).on("keydown", function (e) {
        if (e.key === "Escape") {
            const $bootbox = $(".modal.bootbox:visible");
            if ($bootbox.length > 0) return null;

            const $visibleModals = $(".modal:visible:not(.bootbox)");
            if($visibleModals.length === 1) {
                $visibleModals.hide();
                $("body").css("overflow", "");
            }
        }
    });
}

function gnbMenu() {
    $("#gnb").on("mouseenter", function () {
        $(".navi-layer, .depth2-nav-area").stop(true, true).addClass('active');
    });
    $("#gnb").on("mouseleave", function () {
        $(".navi-layer, .depth2-nav-area").stop(true, true).removeClass('active');
    });
}

function maxChars() {
	// 200자 내외 텍스트 입력
	const maxChars = 200;
	$('#companyDescription').on('input', function () {
		const currentChars = $(this).val().length;
		$('#charCounter i').text(currentChars);
	});
}

function observeModalChanges() {

    const observer = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
            if (mutation.target.style.display === "block") {
                
                // 첫 번째 활성화된 input 찾기
                setTimeout(() => {
                    const inputElement = $(mutation.target).find("input:not([disabled]):not([readonly])[autofocus], textarea, textarea:not([readonly])[autofocus]").first();
                    if (inputElement.length) {
                        inputElement.focus();
                    }
                    $('textarea').each(function () {
                       $(this).scrollTop(0);
                    });
                }, 50); 
            }
        });
    });

    $(".modal").each(function () {
        observer.observe(this, { attributes: true, attributeFilter: ["style"] });
    });
}


function modalScrollHandler() {
    const $modals = $(".modal");
    if ($modals.length === 0) return;

    const observer = new MutationObserver(function () {
        let isModalOpen = $(".modal:visible").length > 0;
        $("body").css("overflow", isModalOpen ? "hidden" : "");
    });

    $modals.each(function () {
        if (this && this.nodeType === 1) {
            observer.observe(this, { attributes: true, attributeFilter: ["style"] });
        }
    });

    // 초기 상태 처리
    $("body").css("overflow", $(".modal:visible").length > 0 ? "hidden" : "");
}


function fileScroll() {
    $('.file-upload-ver02 .file-list').each(function () {
       const $this = $(this);
       const $total = $('.file-upload-ver02 .file-total');

       const observer = new ResizeObserver(() => {

           const scrollHeight = $this[0].scrollHeight;
           const clientHeight = $this[0].clientHeight;

           if (scrollHeight > clientHeight) {
               $this.addClass('has-scroll');
               $total.addClass('scroll-on');
           } else {
               $this.removeClass('has-scroll');
               $total.removeClass('scroll-on');
           }
        });
       observer.observe($this[0]);
    });
}