$(function () {
    let theme = 'theme-jmaa';
    if (/Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(window.navigator.userAgent)) {
        $('body').addClass('mobile');
        $('html').attr('class', theme);
    } else {
        theme = localStorage.getItem('user_theme');
        if (theme) {
            $('html').attr('class', theme);
        }
    }
    let canvas = document.querySelector('canvas'),
        w = window.innerWidth,
        h = window.innerHeight,
        ctx = canvas.getContext("2d"),
        rate = 60, arc = 50, time, count, size = 10, speed = 16, parts = new Array,
        themes = {
            'theme-jmaa': ['#303a50', '#4d5974']
        },
        colors = themes[theme] || ['#303a50', '#4d5974'];
    let mouse = {x: 0, y: 0};
    canvas.setAttribute("width", w);
    canvas.setAttribute("height", h);

    function create() {
        time = 0;
        count = 0;
        for (let a = 0; a < arc; a++) {
            parts[a] = {
                x: Math.ceil(Math.random() * w),
                y: Math.ceil(Math.random() * h),
                toX: Math.random() * 2 - 1,
                toY: Math.random() * 5 - 5,
                c: colors[Math.floor(Math.random() * colors.length)],
                size: Math.random() * size
            }
        }
    }

    function particles() {
        ctx.clearRect(0, 0, w, h);
        canvas.addEventListener("mousemove", MouseMove, false);
        for (let b = 0; b < arc; b++) {
            let c = parts[b];
            let a = DistanceBetween(mouse, parts[b]);
            a = Math.max(Math.min(15 - (a / 10), 10), 1);
            ctx.beginPath();
            ctx.arc(c.x, c.y, c.size * a, 0, Math.PI * 2, false);
            ctx.fillStyle = c.c;
            ctx.strokeStyle = c.c;
            if (b % 2 == 0) {
                ctx.stroke()
            } else {
                ctx.fill()
            }
            c.x = c.x + c.toX * (time * 0.05);
            c.y = c.y + c.toY * (time * 0.05);
            if (c.x > w) {
                c.x = 0
            }
            if (c.y > h) {
                c.y = 0
            }
            if (c.x < 0) {
                c.x = w
            }
            if (c.y < 0) {
                c.y = h
            }
        }
        if (time < speed) {
            time++
        }
        setTimeout(particles, 1000 / rate)
    }

    function MouseMove(a) {
        mouse.x = a.layerX;
        mouse.y = a.layerY
    }

    function DistanceBetween(c, d) {
        let a = d.x - c.x;
        let b = d.y - c.y;
        return Math.sqrt(a * a + b * b)
    }

    function updateSize() {
        w = window.innerWidth;
        h = window.innerHeight;
        canvas.setAttribute("width", w);
        canvas.setAttribute("height", h);
    }

    window.addEventListener('resize', updateSize);
    create();
    particles();
});
