setTimeout(() => {
    window.location.href = `${client_host}/index.html`;
}, 3000);

const secArea = document.getElementById('sec-area');
let count = 0;

function createCircle() {
    if (count >= 3) return;
    
    const circle = document.createElement('div');
    circle.classList.add('circle');
    secArea.appendChild(circle);
    setTimeout(() => {
        circle.style.opacity = '1';
    }, 0);
    
    count++;
    setTimeout(createCircle, 1000);
}

createCircle();