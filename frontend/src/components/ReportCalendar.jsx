import { useMemo, useState } from "react";

const zone = "Asia/Kuala_Lumpur";
const dateFrom = (value) => new Date(`${value}T00:00:00`);
const iso = (date) => new Intl.DateTimeFormat("en-CA", { timeZone: zone }).format(date);
const monday = (value) => { const d = dateFrom(value); const day = d.getDay() || 7; d.setDate(d.getDate() - day + 1); return iso(d); };
const shift = (value, days) => { const d = dateFrom(value); d.setDate(d.getDate() + days); return iso(d); };
const monthKey = (value) => value.slice(0, 7);

function monthDays(month) {
  const start = dateFrom(`${month}-01`); const offset = (start.getDay() + 6) % 7; const days = [];
  for (let i = 0; i < 42; i += 1) { const date = new Date(start); date.setDate(1 - offset + i); days.push(iso(date)); }
  return days;
}

export default function ReportCalendar({ value, onChange, weekly = false, availability = {}, onMonthChange }) {
  const [open, setOpen] = useState(false); const [month, setMonth] = useState(monthKey(value));
  const days = useMemo(() => monthDays(month), [month]);
  const label = weekly ? `${shortDate(monday(value))} – ${shortDate(shift(monday(value), 6))}` : shortDate(value);
  const moveMonth = (amount) => { const d = dateFrom(`${month}-01`); d.setMonth(d.getMonth() + amount); const next = iso(d).slice(0, 7); setMonth(next); onMonthChange?.(next); };
  return <div className="calendar-control"><button type="button" className="calendar-trigger" aria-expanded={open} onClick={() => setOpen((v) => !v)}><span>{weekly ? "Week" : "Date"}</span><strong>{label}</strong><span aria-hidden="true">⌄</span></button>{open && <div className="report-calendar" role="dialog" aria-label={weekly ? "Choose a week" : "Choose a date"}><div className="calendar-head"><button type="button" onClick={() => moveMonth(-1)} aria-label="Previous month">‹</button><strong>{dateFrom(`${month}-01`).toLocaleDateString(undefined, { month: "long", year: "numeric" })}</strong><button type="button" onClick={() => moveMonth(1)} aria-label="Next month">›</button></div><div className="calendar-weekdays">{["M", "T", "W", "T", "F", "S", "S"].map((d, i) => <span key={`${d}-${i}`}>{d}</span>)}</div><div className={`calendar-grid ${weekly ? "is-weekly" : ""}`}>{days.map((date) => { const selected = weekly ? monday(value) === monday(date) : value === date; const inMonth = monthKey(date) === month; const has = weekly ? Array.from({ length: 7 }, (_, i) => availability[shift(monday(date), i)]).some(Boolean) : availability[date]; const today = iso(new Date()) === date; return <button key={date} type="button" className={`${selected ? "is-selected" : ""} ${!inMonth ? "is-muted" : ""} ${today && !selected ? "is-today" : ""}`} onClick={() => { onChange(weekly ? monday(date) : date); setOpen(false); }} aria-label={date} aria-pressed={selected}><span>{date.slice(-2).replace(/^0/, "")}</span>{has && <i aria-label="Reports available" />}</button>; })}</div></div>}</div>;
}
function shortDate(value) { return dateFrom(value).toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" }); }
