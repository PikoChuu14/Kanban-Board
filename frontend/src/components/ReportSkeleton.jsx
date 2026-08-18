export function Skeleton({ className = "" }) { return <span className={`skeleton ${className}`} aria-hidden="true" />; }

export function ReportSkeleton({ weekly = false }) {
  return <section className="dashboard-page report-skeleton" aria-label="Loading report">
    <div className="report-heading"><div><Skeleton className="skeleton-eyebrow" /><Skeleton className="skeleton-title" /><Skeleton className="skeleton-subtitle" /></div><div className="report-actions"><Skeleton className="skeleton-control" /><Skeleton className="skeleton-control" /></div></div>
    {weekly ? <><div className="weekly-overview skeleton-panel"><Skeleton className="skeleton-line medium" /><div className="weekly-kpis">{[1, 2, 3, 4].map((i) => <Skeleton key={i} className="skeleton-kpi" />)}</div></div><div className="skeleton-panel skeleton-days">{[1, 2, 3].map((i) => <div key={i}><Skeleton className="skeleton-line medium" /><Skeleton className="skeleton-card" /></div>)}</div></> : <><div className="team-report-summary skeleton-panel"><Skeleton className="skeleton-line medium" /><Skeleton className="skeleton-line short" /></div><div className="team-report-list">{[1, 2, 3, 4].map((i) => <div className="team-report-card skeleton-panel" key={i}><Skeleton className="skeleton-line medium" /><Skeleton className="skeleton-line" /><Skeleton className="skeleton-line short" /></div>)}</div></>}
  </section>;
}
