import { useEffect, useState } from "react";
import NotificationBell from "./NotificationBell";

const icons = {
  dashboard: <><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></>,
  kanban: <><rect x="3" y="4" width="5" height="16" rx="1" /><rect x="10" y="4" width="5" height="10" rx="1" /><rect x="17" y="4" width="4" height="13" rx="1" /></>,
  team: <><circle cx="9" cy="8" r="3" /><circle cx="17" cy="9" r="2.5" /><path d="M3.5 19c.5-3 2.4-4.5 5.5-4.5s5 1.5 5.5 4.5M14 14.8c3.8-.9 6 .7 6.5 3.7" /></>,
  projects: <><path d="M3 7.5h7l2 2h9v9.5H3z" /><path d="M3 7.5V5h6l2 2.5" /></>,
  reviews: <><path d="M5 5h14v14H5z" /><path d="m8 12 2.5 2.5L16 9" /></>,
  report: <><path d="M6 3h9l3 3v15H6z" /><path d="M9 11h6M9 15h6M9 7h3" /></>,
  menu: <><path d="M4 7h16M4 12h16M4 17h16" /></>,
  chevron: <path d="m9 6 6 6-6 6" />,
};

function Icon({ name, size = 19 }) {
  return <svg className="ui-icon" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{icons[name]}</svg>;
}

function AppShell({ user, activeView, onNavigate, onNotificationNavigate, onLogout, children }) {
  const [collapsed, setCollapsed] = useState(true);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const canSeeTeam = user?.role === "MANAGER" || user?.role === "ADMIN";
  const canSeeProjects = canSeeTeam || user?.role === "STAFF";
  const items = [
    { id: "dashboard", label: "Dashboard", icon: "dashboard" },
    { id: "personal", label: "My Kanban", icon: "kanban" },
    ...(canSeeProjects ? [{ id: "project", label: "Projects", icon: "projects" }] : []),
    ...(canSeeTeam ? [{ id: "staff", label: "Team", icon: "team" }, { id: "reviews", label: "Reviews", icon: "reviews" }] : []),
    { id: "report", label: user?.role === "STAFF" ? "Daily Report" : "Daily Reports", icon: "report" },
  ];

  useEffect(() => {
    const closeOnEscape = (event) => { if (event.key === "Escape") setDrawerOpen(false); };
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, []);

  const navigate = (view) => { onNavigate(view); setDrawerOpen(false); };
  return <div className={`app-shell ${collapsed ? "sidebar-collapsed" : ""}`}>
    <div className={`sidebar-overlay ${drawerOpen ? "is-visible" : ""}`} onClick={() => setDrawerOpen(false)} />
    <aside className={`sidebar ${drawerOpen ? "drawer-open" : ""}`}>
      <div className="sidebar-brand">
        <div className="brand-mark">K<span /></div>
        <div className="brand-copy"><strong>Kovax</strong><span>FlowOps</span></div>
      </div>
      <div className="sidebar-section-label">Workspace</div>
      <nav className="sidebar-nav" aria-label="Primary navigation">
        {items.map((item) => <button key={item.id} type="button" title={collapsed ? item.label : undefined} className={activeView === item.id ? "is-active" : ""} onClick={() => navigate(item.id)}><Icon name={item.icon} /><span>{item.label}</span>{activeView === item.id && <i />}</button>)}
      </nav>
      <div className="sidebar-spacer" />
      <div className="sidebar-profile" title={`${user?.name || user?.email || "User"} · ${user?.role || ""}${user?.departmentName ? ` · ${user.departmentName}` : ""}`}><div className="avatar">{(user?.name || user?.email || "K").slice(0, 1).toUpperCase()}</div><div className="profile-copy"><strong>{user?.name || user?.email || "User"}</strong><span>{user?.role}{user?.departmentName ? ` · ${user.departmentName}` : ""}</span></div></div>
      <button type="button" className="sidebar-logout" onClick={onLogout}><span className="logout-symbol">↪</span><span>Sign out</span></button>
      <button type="button" className="sidebar-collapse" aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"} onClick={() => setCollapsed((value) => !value)}><Icon name="chevron" /><span>{collapsed ? "Expand" : "Collapse"}</span></button>
    </aside>
    <main className="app-main">
      <NotificationBell onNavigate={onNotificationNavigate} />
      <header className="mobile-topbar"><button type="button" className="btn-icon" aria-label="Open navigation" onClick={() => setDrawerOpen(true)}><Icon name="menu" /></button><div className="mobile-brand"><div className="brand-mark">K<span /></div><strong>Kovax FlowOps</strong></div><div className="avatar">{user?.name?.slice(0, 1)?.toUpperCase() || "K"}</div></header>
      <div className="app-content">{children}</div>
    </main>
  </div>;
}

export default AppShell;
