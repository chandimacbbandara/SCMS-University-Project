// Navigation and Menu Handling
document.addEventListener('DOMContentLoaded', function() {
    const hamburger = document.getElementById('hamburger');
    const navMenu = document.getElementById('navMenu');

    // Hamburger Menu Toggle
    if (hamburger) {
        hamburger.addEventListener('click', function() {
            navMenu.classList.toggle('active');
        });
    }

    // Close menu when a link is clicked
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            navMenu.classList.remove('active');
            
            // Update active link
            navLinks.forEach(l => l.classList.remove('active'));
            this.classList.add('active');
        });
    });

    // Smooth scrolling
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            const href = this.getAttribute('href');
            if (href.startsWith('#')) {
                e.preventDefault();
                const targetId = href.substring(1);
                scrollToSection(targetId);
            }
        });
    });
});

// Navigate to different sections
function navigateTo(sectionId) {
    scrollToSection(sectionId);
    
    // Update active nav link
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
        link.classList.remove('active');
        if (link.getAttribute('href') === '#' + sectionId) {
            link.classList.add('active');
        }
    });
}

// Smooth scroll to section
function scrollToSection(sectionId) {
    const section = document.getElementById(sectionId);
    if (section) {
        section.scrollIntoView({ behavior: 'smooth' });
    } else {
        // If section doesn't exist, show alert or create it dynamically
        console.log(`Section ${sectionId} not found. Redirecting would happen here.`);
        showPlaceholder(sectionId);
    }
}

// Show placeholder for future pages
function showPlaceholder(sectionId) {
    const messages = {
        'report': 'Redirecting to Report Concern page...',
        'concerns': 'Redirecting to My Concerns page...',
        'status': 'Redirecting to Track Status page...',
        'contact': 'Redirecting to Contact page...',
        'profile': 'Redirecting to Profile page...'
    };
    
    alert(messages[sectionId] || 'Redirecting...');
}

// Action cards click handling
document.addEventListener('DOMContentLoaded', function() {
    const actionCards = document.querySelectorAll('.action-card');
    actionCards.forEach(card => {
        card.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-10px) scale(1.05)';
        });
        card.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0) scale(1)';
        });
    });
});

// Add keyboard navigation
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        const navMenu = document.getElementById('navMenu');
        if (navMenu) {
            navMenu.classList.remove('active');
        }
    }
});

// Back to top button (optional - for future use)
function scrollToTop() {
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// Console greeting
console.log('%cWelcome to Student Concern Management System!', 'color: #667eea; font-size: 16px; font-weight: bold;');
console.log('%cVersion 1.0', 'color: #764ba2; font-size: 12px;');
