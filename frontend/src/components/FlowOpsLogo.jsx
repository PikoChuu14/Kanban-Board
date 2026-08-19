function FlowOpsLogo({ className = "", title = "FlowOps", decorative = false }) {
  const labelled = decorative ? {} : { role: "img", "aria-label": title };

  return (
    <svg
      className={`flowops-logo ${className}`.trim()}
      viewBox="0 0 48 48"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden={decorative || undefined}
      focusable="false"
      {...labelled}
    >
      <rect width="48" height="48" rx="13" fill="#12344C" />
      <path d="M13 15.5h9.5c3.6 0 5.4 2.1 5.4 5.3v6.4c0 3.3 1.8 5.3 5.4 5.3H36" stroke="#57C3D2" strokeWidth="4" strokeLinecap="round" />
      <circle cx="13" cy="15.5" r="4.2" fill="#F8FBFF" />
      <circle cx="27.9" cy="24" r="4.2" fill="#57C3D2" stroke="#12344C" strokeWidth="2" />
      <circle cx="36" cy="32.5" r="4.2" fill="#F8FBFF" />
    </svg>
  );
}

export default FlowOpsLogo;
