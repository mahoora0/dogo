document.addEventListener('DOMContentLoaded', function() {
    lucide.createIcons();

    // Responsive Mobile Sidebar Toggle Logic
    const mobileMenuBtn = document.getElementById('mobileMenuBtn');
    const closeSidebarBtn = document.getElementById('closeSidebarBtn');
    const adminSidebar = document.getElementById('adminSidebar');
    const sidebarBackdrop = document.getElementById('sidebarBackdrop');

    function openSidebar() {
        if (adminSidebar && sidebarBackdrop) {
            adminSidebar.classList.toggle('hidden');
            sidebarBackdrop.classList.remove('hidden');
            setTimeout(() => {
                sidebarBackdrop.classList.remove('opacity-0');
                sidebarBackdrop.classList.add('opacity-100');
            }, 50);
        }
    }

    function closeSidebar() {
        if (adminSidebar && sidebarBackdrop) {
            adminSidebar.classList.add('hidden');
            sidebarBackdrop.classList.remove('opacity-100');
            sidebarBackdrop.classList.add('opacity-0');
            setTimeout(() => {
                sidebarBackdrop.classList.add('hidden');
            }, 300);
        }
    }

    if (mobileMenuBtn) {
        mobileMenuBtn.addEventListener('click', openSidebar);
    }

    if (closeSidebarBtn) {
        closeSidebarBtn.addEventListener('click', closeSidebar);
    }

    if (sidebarBackdrop) {
        sidebarBackdrop.addEventListener('click', closeSidebar);
    }
});
