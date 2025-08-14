document.addEventListener('DOMContentLoaded', function () {
    const sections = document.querySelectorAll('section, article[id]');
    const navLinks = document.querySelectorAll('#navigation .nav-link, #navigation .nav-sublink');
    const nav = document.getElementById('navigation');

    function copyCode() {
        const codeBlock = this.parentElement;
        const code = codeBlock.querySelector('code').innerText;
        navigator.clipboard.writeText(code).then(() => {
            this.textContent = 'Copied!';
            setTimeout(() => {
                this.textContent = 'Copy';
            }, 2000);
        }).catch(err => {
            console.error('Failed to copy: ', err);
        });
    }

    document.querySelectorAll('.copy-btn').forEach(btn => {
        btn.addEventListener('click', copyCode);
    });

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const id = entry.target.getAttribute('id');
                const activeLink = document.querySelector(`a[href="#${id}"]`);

                navLinks.forEach(link => {
                    link.classList.remove('active');
                });

                if (activeLink) {
                    activeLink.classList.add('active');
                    // If it's a sublink, also activate its parent
                    const parentLink = activeLink.closest('div').querySelector('.nav-link');
                    if (parentLink && !parentLink.classList.contains('active')) {
                        parentLink.classList.add('active');
                    }
                }
            }
        });
    }, { rootMargin: "-50% 0px -50% 0px", threshold: 0 });

    sections.forEach(section => {
        observer.observe(section);
    });

    nav.addEventListener('click', function (e) {
        if (e.target.tagName === 'A') {
            e.preventDefault();
            const targetId = e.target.getAttribute('href');
            const targetElement = document.querySelector(targetId);
            if (targetElement) {
                targetElement.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        }
    });
});