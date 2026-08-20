import { useState } from "react";
import { usePwaInstall } from "../hooks/usePwaInstall";

function InstallFlowOps({ compact = false }) {
  const { canInstall, installed, isIos, install } = usePwaInstall();
  const [showIosHelp, setShowIosHelp] = useState(false);

  if (installed || (!canInstall && !isIos)) return null;

  return (
    <div className={compact ? "pwa-install pwa-install-compact" : "pwa-install"}>
      <button
        type="button"
        className="secondary-button"
        onClick={() => isIos ? setShowIosHelp((visible) => !visible) : install()}
      >
        Install FlowOps
      </button>
      {isIos && showIosHelp && (
        <div className="ios-install-help" role="status">
          <strong>Install FlowOps on this device</strong>
          <ol>
            <li>Tap Share in Safari.</li>
            <li>Choose Add to Home Screen.</li>
            <li>Open FlowOps from the Home Screen.</li>
          </ol>
        </div>
      )}
    </div>
  );
}

export default InstallFlowOps;
