document.addEventListener("DOMContentLoaded", () => {
  if (!document.body.classList.contains("docs-page")) {
    return;
  }

  const sidebar = document.querySelector(".sidebar[data-nav-level='docs']");
  if (!sidebar) {
    return;
  }

  const items = [
    { href: "01-proposal.html", number: "01", label: "\u4f01\u753b\u63d0\u6848\u66f8" },
    { href: "02-market-research.html", number: "02", label: "\u30de\u30fc\u30b1\u30c3\u30c8\u30ea\u30b5\u30fc\u30c1" },
    { href: "03-persona.html", number: "03", label: "\u30da\u30eb\u30bd\u30ca\u30b7\u30fc\u30c8" },
    { href: "04-sitemap.html", number: "04", label: "\u30b5\u30a4\u30c8\u30de\u30c3\u30d7" },
    { href: "05-wireframe.html", number: "05", label: "\u30ef\u30a4\u30e4\u30fc\u30d5\u30ec\u30fc\u30e0" },
    { href: "06-design-guide.html", number: "06", label: "\u30c7\u30b6\u30a4\u30f3\u30ac\u30a4\u30c9\u30e9\u30a4\u30f3" },
    { href: "07-specification.html", number: "07", label: "\u4ed5\u69d8\u66f8" },
    { href: "08-db-design.html", number: "08", label: "DB\u8a2d\u8a08\u66f8" },
    { href: "09-test-report.html", number: "09", label: "\u30c6\u30b9\u30c8\u5831\u544a\u66f8" },
    { href: "10-retrospective.html", number: "10", label: "\u632f\u308a\u8fd4\u308a\u30fb\u6280\u8853\u8a18\u4e8b" }
  ];

  const links = items
    .map(
      (item) =>
        `<a href="${item.href}"><span class="nav-number">${item.number}</span> ${item.label}</a>`
    )
    .join("");

  sidebar.innerHTML = `
    <div class="sidebar-header">
      <div class="logo">PROJECT ARCHIVE</div>
      <div class="project-name">\u30d7\u30ed\u30b8\u30a7\u30af\u30c8\u30a2\u30fc\u30ab\u30a4\u30d6</div>
    </div>
    <nav class="sidebar-nav">
      <div class="nav-group">
        <div class="nav-group-title">\u5236\u4f5c\u30c9\u30ad\u30e5\u30e1\u30f3\u30c8</div>
        ${links}
      </div>
    </nav>
    <div class="sidebar-footer">&copy; Project Archive 2026</div>
  `;

  const currentPage = window.location.pathname.split("/").pop() || "01-proposal.html";
  sidebar.querySelectorAll(".sidebar-nav a").forEach((link) => {
    const href = link.getAttribute("href");
    if (href === currentPage) {
      link.classList.add("active");
      link.setAttribute("aria-current", "page");
    }
  });

  const hamburger = document.querySelector(".hamburger");
  const closeSidebar = () => sidebar.classList.remove("open");

  if (hamburger) {
    hamburger.addEventListener("click", (event) => {
      event.stopPropagation();
      sidebar.classList.toggle("open");
    });

    document.addEventListener("click", (event) => {
      if (sidebar.classList.contains("open") && !sidebar.contains(event.target)) {
        closeSidebar();
      }
    });

    sidebar.querySelectorAll("a").forEach((link) => {
      link.addEventListener("click", () => {
        if (window.innerWidth <= 768) {
          closeSidebar();
        }
      });
    });
  }
});
