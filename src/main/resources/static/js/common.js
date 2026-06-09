function hideLogo() {
    document.getElementById('logoImg').style.display = 'none';
    document.getElementById('logoText').style.display = 'inline-block';
}

document.addEventListener('DOMContentLoaded', function() {
  const navItems = document.querySelectorAll('.nav-item');
  navItems.forEach(item => {
    const category = item.querySelector('.nav-category');
    if (category) {
      category.addEventListener('click', function(e) {
        if (window.innerWidth <= 820) {
          e.preventDefault();
          e.stopPropagation();
          
          const isActive = item.classList.contains('mobile-active');
          
          navItems.forEach(otherItem => {
            otherItem.classList.remove('mobile-active');
          });
          
          if (!isActive) {
            item.classList.add('mobile-active');
          }
        }
      });
    }
    
    const dropdown = item.querySelector('.dropdown-menu');
    if (dropdown) {
      dropdown.addEventListener('click', function(e) {
        e.stopPropagation();
      });
    }
  });
  
  document.addEventListener('click', function() {
    navItems.forEach(item => {
      item.classList.remove('mobile-active');
    });
  });

  // Mobile Drawer Toggle
  const mobileMenuBtn = document.getElementById('mobileMenuBtn');
  const mobileDrawer = document.getElementById('mobileDrawer');
  const mobileDrawerClose = document.getElementById('mobileDrawerClose');
  
  if (mobileMenuBtn && mobileDrawer) {
    mobileMenuBtn.addEventListener('click', function(e) {
      e.stopPropagation();
      mobileDrawer.classList.add('open');
      document.body.style.overflow = 'hidden';
    });
  }
  
  if (mobileDrawerClose && mobileDrawer) {
    mobileDrawerClose.addEventListener('click', function() {
      mobileDrawer.classList.remove('open');
      document.body.style.overflow = '';
    });
  }
  
  if (mobileDrawer) {
    mobileDrawer.addEventListener('click', function(e) {
      if (e.target === mobileDrawer) {
        mobileDrawer.classList.remove('open');
        document.body.style.overflow = '';
      }
    });
  }

  // Mobile Drawer Accordion Menu Toggles
  const mobileNavGroups = document.querySelectorAll('.mobile-nav-group');
  mobileNavGroups.forEach(group => {
    const toggleBtn = group.querySelector('.mobile-nav-toggle');
    if (toggleBtn) {
      toggleBtn.addEventListener('click', function(e) {
        e.stopPropagation();
        const isActive = group.classList.contains('active');
        mobileNavGroups.forEach(otherGroup => {
          otherGroup.classList.remove('active');
        });
        if (!isActive) {
          group.classList.add('active');
        }
      });
    }
  });
});