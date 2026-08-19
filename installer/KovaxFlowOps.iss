#define AppName "Kovax FlowOps"
#define AppVersion "1.0.0"
#define AppPublisher "Kovax"
#define AppId "{8B58D1C2-7FD1-4CF7-9B49-0B2AE24C1A4E}"

[Setup]
AppId={{#AppId}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
DefaultDirName={autopf}\Kovax FlowOps
DefaultGroupName=Kovax FlowOps
OutputDir=..\dist\installer
OutputBaseFilename=KovaxFlowOps-Setup
UninstallDisplayName=Kovax FlowOps
PrivilegesRequired=admin
ArchitecturesInstallIn64BitMode=x64compatible
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
VersionInfoVersion={#AppVersion}.0
VersionInfoDescription=Kovax FlowOps installer

[Files]
Source: "payload\app\*"; DestDir: "{app}\app"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "payload\runtime\*"; DestDir: "{app}\runtime"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "payload\KovaxFlowOps.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "payload\KovaxFlowOps.xml"; DestDir: "{app}"; Flags: ignoreversion
Source: "payload\tools\*"; DestDir: "{app}\tools"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "payload\prerequisites\postgresql-installer.exe"; DestDir: "{tmp}"; Flags: deleteafterinstall
Source: "scripts\detect-postgresql.ps1"; DestDir: "{tmp}"; Flags: dontcopy
Source: "..\START_HERE.txt"; DestDir: "{app}\docs"; Flags: ignoreversion
Source: "..\DEPLOYMENT.md"; DestDir: "{app}\docs"; Flags: ignoreversion
Source: "..\BACKUP_RESTORE.md"; DestDir: "{app}\docs"; Flags: ignoreversion
Source: "..\TROUBLESHOOTING.md"; DestDir: "{app}\docs"; Flags: ignoreversion
Source: "..\ADMIN_GUIDE.md"; DestDir: "{app}\docs"; Flags: ignoreversion
Source: "..\VERSION.txt"; DestDir: "{app}\docs"; Flags: ignoreversion

[Dirs]
Name: "{commonappdata}\Kovax FlowOps\config"
Name: "{commonappdata}\Kovax FlowOps\logs"
Name: "{commonappdata}\Kovax FlowOps\backups"
Name: "{commonappdata}\Kovax FlowOps\runtime"

[Icons]
Name: "{group}\Open Kovax FlowOps"; Filename: "{sys}\cmd.exe"; Parameters: "/c start http://localhost:{code:GetPort}"
Name: "{group}\Backup Kovax FlowOps"; Filename: "{sys}\WindowsPowerShell\v1.0\powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -File ""{app}\tools\backup-installed.ps1"""
Name: "{group}\Restore Kovax FlowOps"; Filename: "{sys}\WindowsPowerShell\v1.0\powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -File ""{app}\tools\restore-installed.ps1"""
Name: "{group}\Kovax FlowOps Documentation"; Filename: "{app}\docs"
Name: "{commondesktop}\Kovax FlowOps"; Filename: "{sys}\cmd.exe"; Parameters: "/c start http://localhost:{code:GetPort}"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional shortcuts:"

[UninstallRun]
Filename: "{app}\KovaxFlowOps.exe"; Parameters: "stop"; Flags: runhidden waituntilterminated
Filename: "{app}\KovaxFlowOps.exe"; Parameters: "uninstall"; Flags: runhidden waituntilterminated
Filename: "{sys}\netsh.exe"; Parameters: "advfirewall firewall delete rule name=""Kovax FlowOps"""; Flags: runhidden waituntilterminated

[Code]
var
  DbChoicePage: TWizardPage;
  DbIntroLabel: TNewStaticText;
  DbInstalledRadio: TNewRadioButton;
  DbAutomaticRadio: TNewRadioButton;
  DbAdminPage: TInputQueryWizardPage;
  AdminPage: TInputQueryWizardPage;
  DbStatusLabel: TNewStaticText;
  PostgreSQLDetected: Boolean;
  InstallPostgres: Boolean;
  PgBinDir: String;
  PgServiceName: String;
  PgServiceStatus: String;
  PgMajorVersion: String;
  PgDetectionMessage: String;
  PgDetectionSummary: String;
  PgDiagnosticSummary: String;
  AppPort: String;
  DataRoot: String;
  StartupTitleLabel: TNewStaticText;
  StartupSpinner: TNewStaticText;
  StartupStatusLabel: TNewStaticText;
  StartupExplanationLabel: TNewStaticText;
  StartupOpenButton: TNewButton;
  StartupRetryButton: TNewButton;
  StartupLogsButton: TNewButton;
  StartupTimerID: LongWord;
  StartupSpinnerFrame: Integer;
  StartupTimerTicks: Integer;
  StartupAttempt: Integer;
  StartupStateFile: String;
  StartupReady: Boolean;
  StartupTimedOut: Boolean;

function SetTimer(hWnd, nIDEvent, uElapse, lpTimerFunc: LongWord): LongWord;
  external 'SetTimer@user32.dll stdcall';
function KillTimer(hWnd, nIDEvent: LongWord): Boolean;
  external 'KillTimer@user32.dll stdcall';

function PsPath: String;
begin Result := ExpandConstant('{sys}\WindowsPowerShell\v1.0\powershell.exe'); end;

function JsonEscape(S: String): String;
begin
  StringChangeEx(S, '\', '\\', True); StringChangeEx(S, '"', '\"', True);
  StringChangeEx(S, #13, '\r', True); StringChangeEx(S, #10, '\n', True); Result := S;
end;

function GetPort(Param: String): String;
begin Result := AppPort; end;

function BooleanText(Value: Boolean): String;
begin
  if Value then Result := 'True' else Result := 'False';
end;

procedure LayoutQueryPage(Page: TInputQueryWizardPage);
var
  I, Margin, LabelWidth, EditLeft, RowTop, RowHeight: Integer;
begin
  Margin := ScaleX(16);
  LabelWidth := ScaleX(150);
  EditLeft := Margin + LabelWidth + ScaleX(10);
  RowHeight := ScaleY(30);

  Page.SubCaptionLabel.Left := Margin;
  Page.SubCaptionLabel.Width := Page.SurfaceWidth - (Margin * 2);
  Page.SubCaptionLabel.AutoSize := False;
  Page.SubCaptionLabel.WordWrap := True;
  Page.SubCaptionLabel.Height := ScaleY(38);
  RowTop := Page.SubCaptionLabel.Top + Page.SubCaptionLabel.Height + ScaleY(10);

  for I := 0 to 3 do
  begin
    Page.PromptLabels[I].Left := Margin;
    Page.PromptLabels[I].Top := RowTop + (I * RowHeight);
    Page.PromptLabels[I].Width := LabelWidth;
    Page.PromptLabels[I].Height := RowHeight - ScaleY(4);
    Page.PromptLabels[I].AutoSize := False;
    Page.PromptLabels[I].WordWrap := True;

    Page.Edits[I].Left := EditLeft;
    Page.Edits[I].Top := RowTop + (I * RowHeight);
    Page.Edits[I].Width := Page.SurfaceWidth - EditLeft - Margin;
    Page.Edits[I].Height := ScaleY(23);
  end;
end;

procedure LayoutDatabasePage;
var
  Margin, RadioLeft, RadioTop, RadioHeight, StatusTop, StatusHeight: Integer;
begin
  Margin := ScaleX(16);
  RadioLeft := ScaleX(28);
  RadioHeight := ScaleY(28);

  DbIntroLabel.Left := Margin;
  DbIntroLabel.Top := ScaleY(4);
  DbIntroLabel.Width := DbChoicePage.SurfaceWidth - (Margin * 2);
  DbIntroLabel.AutoSize := False;
  DbIntroLabel.WordWrap := True;
  DbIntroLabel.Height := ScaleY(38);

  RadioTop := DbIntroLabel.Top + DbIntroLabel.Height + ScaleY(10);
  DbInstalledRadio.Left := RadioLeft;
  DbInstalledRadio.Top := RadioTop;
  DbInstalledRadio.Width := DbChoicePage.SurfaceWidth - RadioLeft - ScaleX(24);
  DbInstalledRadio.Height := RadioHeight;

  DbAutomaticRadio.Left := RadioLeft;
  DbAutomaticRadio.Top := RadioTop + RadioHeight + ScaleY(12);
  DbAutomaticRadio.Width := DbChoicePage.SurfaceWidth - RadioLeft - ScaleX(24);
  DbAutomaticRadio.Height := RadioHeight;

  DbStatusLabel.Left := Margin;
  StatusTop := DbAutomaticRadio.Top + DbAutomaticRadio.Height + ScaleY(12);
  DbStatusLabel.Top := StatusTop;
  DbStatusLabel.Width := DbChoicePage.SurfaceWidth - (Margin * 2);
  StatusHeight := DbChoicePage.SurfaceHeight - StatusTop - Margin;
  if StatusHeight < ScaleY(30) then StatusHeight := ScaleY(30);
  DbStatusLabel.Height := StatusHeight;
  DbStatusLabel.AutoSize := False;
  DbStatusLabel.WordWrap := True;
end;

procedure UpdateStartupSpinner;
begin
  case StartupSpinnerFrame mod 4 of
    0: StartupSpinner.Caption := #9680;
    1: StartupSpinner.Caption := #9683;
    2: StartupSpinner.Caption := #9681;
    3: StartupSpinner.Caption := #9682;
  end;
end;

procedure StopStartupTimer;
begin
  if StartupTimerID <> 0 then
  begin
    KillTimer(0, StartupTimerID);
    StartupTimerID := 0;
  end;
end;

procedure OpenKovaxFlowOps(Sender: TObject);
var
  ErrorCode: Integer;
begin
  ShellExec('', 'http://localhost:' + AppPort, '', '', SW_SHOWNORMAL, ewNoWait, ErrorCode);
end;

procedure OpenKovaxLogs(Sender: TObject);
var
  ErrorCode: Integer;
begin
  ShellExec('', DataRoot + '\logs', '', '', SW_SHOWNORMAL, ewNoWait, ErrorCode);
end;

procedure StartupTimerTick(HWnd, Msg, TimerID, SysTime: LongWord); forward;

procedure StartReadinessCheck;
var
  Code: Integer;
  Parameters: String;
begin
  StartupAttempt := StartupAttempt + 1;
  StartupStateFile := ExpandConstant('{tmp}\flowops-ready-') + IntToStr(StartupAttempt) + '.txt';
  DeleteFile(StartupStateFile);
  StartupReady := False;
  StartupTimedOut := False;
  StartupTimerTicks := 0;
  StartupSpinnerFrame := 0;
  StartupTitleLabel.Caption := 'Starting Kovax FlowOps';
  StartupStatusLabel.Caption := 'Starting Windows service...';
  StartupExplanationLabel.Caption := 'This may take up to 90 seconds on first launch.';
  StartupOpenButton.Visible := False;
  StartupRetryButton.Visible := False;
  StartupLogsButton.Visible := False;
  WizardForm.NextButton.Enabled := False;
  StartupSpinner.Visible := True;
  UpdateStartupSpinner;
  StopStartupTimer;
  StartupTimerID := SetTimer(0, 0, 100, CreateCallback(@StartupTimerTick));

  Parameters := '-NoProfile -ExecutionPolicy Bypass -File "' +
    ExpandConstant('{app}\tools\wait-for-ready.ps1') + '" -Url "http://localhost:' +
    AppPort + '/api/health" -StateFile "' + StartupStateFile +
    '" -TimeoutSeconds 90 -IntervalMilliseconds 1500';
  if not Exec(PsPath, Parameters, '', SW_HIDE, ewNoWait, Code) then
  begin
    StopStartupTimer;
    StartupSpinner.Visible := False;
    StartupTimedOut := True;
    StartupTitleLabel.Caption := 'Kovax FlowOps is taking longer than expected to start.';
    StartupStatusLabel.Caption := 'The readiness check could not be started.';
    StartupExplanationLabel.Caption := 'The service may still be starting or may require troubleshooting.';
    StartupRetryButton.Visible := True;
    StartupLogsButton.Visible := True;
    WizardForm.NextButton.Enabled := True;
  end;
end;

procedure RetryReadinessCheck(Sender: TObject);
begin
  StartReadinessCheck;
end;

procedure StartupTimerTick(HWnd, Msg, TimerID, SysTime: LongWord);
var
  StateData: AnsiString;
  StateText: String;
begin
  StartupTimerTicks := StartupTimerTicks + 1;
  StartupSpinnerFrame := (StartupSpinnerFrame + 1) mod 8;
  UpdateStartupSpinner;

  if StartupTimerTicks < 20 then
    StartupStatusLabel.Caption := 'Starting Windows service...'
  else if StartupTimerTicks < 55 then
    StartupStatusLabel.Caption := 'Connecting to database...'
  else if StartupTimerTicks < 100 then
    StartupStatusLabel.Caption := 'Preparing application...'
  else
    StartupStatusLabel.Caption := 'Waiting for Kovax FlowOps to become ready...';

  { Read the worker result once per second; this never performs network I/O on the UI thread. }
  if ((StartupTimerTicks mod 10) <> 0) or
    (not LoadStringFromFile(StartupStateFile, StateData)) then Exit;
  StateText := StateData;

  if Pos('STATE=READY', StateText) > 0 then
  begin
    StartupReady := True;
    StopStartupTimer;
    StartupSpinner.Visible := False;
    StartupTitleLabel.Caption := #10003 + ' Kovax FlowOps is ready';
    StartupStatusLabel.Caption := 'The application has started successfully.';
    StartupExplanationLabel.Caption := 'You can open Kovax FlowOps now, or finish setup and open it later from the shortcut.';
    StartupOpenButton.Visible := True;
    WizardForm.NextButton.Enabled := True;
  end
  else if Pos('STATE=TIMEOUT', StateText) > 0 then
  begin
    StartupTimedOut := True;
    StopStartupTimer;
    StartupSpinner.Visible := False;
    StartupTitleLabel.Caption := 'Kovax FlowOps is taking longer than expected to start.';
    StartupStatusLabel.Caption := 'The service may still be starting or may require troubleshooting.';
    StartupExplanationLabel.Caption := 'You can retry the readiness check or open the logs for more information.';
    StartupRetryButton.Visible := True;
    StartupLogsButton.Visible := True;
    WizardForm.NextButton.Enabled := True;
  end;
end;

procedure CreateStartupControls;
var
  PageWidth, CenterX, ButtonTop: Integer;
begin
  PageWidth := WizardForm.FinishedPage.ClientWidth;
  CenterX := PageWidth div 2;

  StartupTitleLabel := TNewStaticText.Create(WizardForm.FinishedPage);
  StartupTitleLabel.Parent := WizardForm.FinishedPage;
  StartupTitleLabel.Left := ScaleX(24);
  StartupTitleLabel.Top := ScaleY(24);
  StartupTitleLabel.Width := PageWidth - ScaleX(48);
  StartupTitleLabel.Height := ScaleY(36);
  StartupTitleLabel.AutoSize := False;
  StartupTitleLabel.Alignment := taCenter;
  StartupTitleLabel.Font.Size := 14;
  StartupTitleLabel.Font.Style := [fsBold];

  StartupSpinner := TNewStaticText.Create(WizardForm.FinishedPage);
  StartupSpinner.Parent := WizardForm.FinishedPage;
  StartupSpinner.Width := ScaleX(58);
  StartupSpinner.Height := ScaleY(58);
  StartupSpinner.Left := CenterX - (StartupSpinner.Width div 2);
  StartupSpinner.Top := ScaleY(76);
  StartupSpinner.AutoSize := False;
  StartupSpinner.Alignment := taCenter;
  StartupSpinner.Font.Name := 'Segoe UI Symbol';
  StartupSpinner.Font.Size := 28;
  StartupSpinner.Font.Color := clHighlight;

  StartupStatusLabel := TNewStaticText.Create(WizardForm.FinishedPage);
  StartupStatusLabel.Parent := WizardForm.FinishedPage;
  StartupStatusLabel.Left := ScaleX(24);
  StartupStatusLabel.Top := ScaleY(148);
  StartupStatusLabel.Width := PageWidth - ScaleX(48);
  StartupStatusLabel.Height := ScaleY(34);
  StartupStatusLabel.AutoSize := False;
  StartupStatusLabel.Alignment := taCenter;
  StartupStatusLabel.Font.Size := 10;

  StartupExplanationLabel := TNewStaticText.Create(WizardForm.FinishedPage);
  StartupExplanationLabel.Parent := WizardForm.FinishedPage;
  StartupExplanationLabel.Left := ScaleX(40);
  StartupExplanationLabel.Top := ScaleY(190);
  StartupExplanationLabel.Width := PageWidth - ScaleX(80);
  StartupExplanationLabel.Height := ScaleY(48);
  StartupExplanationLabel.AutoSize := False;
  StartupExplanationLabel.WordWrap := True;
  StartupExplanationLabel.Alignment := taCenter;

  ButtonTop := ScaleY(252);
  StartupOpenButton := TNewButton.Create(WizardForm.FinishedPage);
  StartupOpenButton.Parent := WizardForm.FinishedPage;
  StartupOpenButton.Caption := 'Open Kovax FlowOps';
  StartupOpenButton.Width := ScaleX(150);
  StartupOpenButton.Height := ScaleY(28);
  StartupOpenButton.Left := CenterX - (StartupOpenButton.Width div 2);
  StartupOpenButton.Top := ButtonTop;
  StartupOpenButton.OnClick := @OpenKovaxFlowOps;

  StartupRetryButton := TNewButton.Create(WizardForm.FinishedPage);
  StartupRetryButton.Parent := WizardForm.FinishedPage;
  StartupRetryButton.Caption := 'Retry';
  StartupRetryButton.Width := ScaleX(100);
  StartupRetryButton.Height := ScaleY(28);
  StartupRetryButton.Left := CenterX - ScaleX(108);
  StartupRetryButton.Top := ButtonTop;
  StartupRetryButton.OnClick := @RetryReadinessCheck;

  StartupLogsButton := TNewButton.Create(WizardForm.FinishedPage);
  StartupLogsButton.Parent := WizardForm.FinishedPage;
  StartupLogsButton.Caption := 'Open Logs';
  StartupLogsButton.Width := ScaleX(100);
  StartupLogsButton.Height := ScaleY(28);
  StartupLogsButton.Left := CenterX + ScaleX(8);
  StartupLogsButton.Top := ButtonTop;
  StartupLogsButton.OnClick := @OpenKovaxLogs;

  StartupOpenButton.Visible := False;
  StartupRetryButton.Visible := False;
  StartupLogsButton.Visible := False;

end;

function DetectionValue(const TextData, Key: String): String;
var
  Marker: String;
  StartAt, EndAt, Remaining: Integer;
begin
  Marker := Key + '=';
  StartAt := Pos(Marker, TextData);
  if StartAt = 0 then begin Result := ''; Exit; end;
  StartAt := StartAt + Length(Marker);
  Remaining := Length(TextData) - StartAt + 1;
  EndAt := Pos(#10, Copy(TextData, StartAt, Remaining));
  if EndAt > 0 then Result := Copy(TextData, StartAt, EndAt - 1) else Result := Copy(TextData, StartAt, Remaining);
  StringChangeEx(Result, #13, '', True);
end;

function DetectPostgres: Boolean;
  var OutFile, DetectionScript, OutputText, ExecParameters: String;
      Output: AnsiString; Code: Integer; ExecStarted: Boolean;
begin
  Log('PostgreSQL detection started');
  Log('Installer architecture: IsWin64=' + BooleanText(IsWin64) +
    '; Is64BitInstallMode=' + BooleanText(Is64BitInstallMode));
  Log('Installer elevation: IsAdmin=' + BooleanText(IsAdmin) +
    '; IsAdminInstallMode=' + BooleanText(IsAdminInstallMode));
  ExtractTemporaryFile('detect-postgresql.ps1');
  DetectionScript := ExpandConstant('{tmp}\detect-postgresql.ps1');
  OutFile := ExpandConstant('{tmp}\flowops-postgres.txt');
  DeleteFile(OutFile);
  ExecParameters := '-NoProfile -ExecutionPolicy Bypass -File "' + DetectionScript +
    '" -OutputFile "' + OutFile + '"';
  ExecStarted := Exec(PsPath, ExecParameters, '', SW_HIDE, ewWaitUntilTerminated, Code);
  Log('Detection helper started: ' + BooleanText(ExecStarted));
  Log('Detection helper exit code: ' + IntToStr(Code));
  Log('Detection output file exists: ' + BooleanText(FileExists(OutFile)));
  PgBinDir := ''; PgServiceName := ''; PgServiceStatus := ''; PgMajorVersion := '';
  PgDetectionMessage := 'PostgreSQL was not detected. The bundled PostgreSQL package will be installed.';
  PgDetectionSummary := PgDetectionMessage;
  PgDiagnosticSummary := 'Detection: Registry=No | Service=No | Filesystem=No | PATH=No';
  if (not ExecStarted) or (Code <> 0) or not LoadStringFromFile(OutFile, Output) then
  begin
    Log('PostgreSQL detection output could not be loaded');
    Log('PostgreSQLDetected=False');
    Result := False;
    Exit;
  end;
  OutputText := Output;
  PgBinDir := DetectionValue(OutputText, 'BIN_DIR');
  PgServiceName := DetectionValue(OutputText, 'SERVICE_NAME');
  PgServiceStatus := DetectionValue(OutputText, 'SERVICE_STATUS');
  PgMajorVersion := DetectionValue(OutputText, 'MAJOR_VERSION');
  PgDetectionMessage := DetectionValue(OutputText, 'MESSAGE');
  PgDetectionSummary := PgDetectionMessage;
  StringChangeEx(PgDetectionSummary, '|', #13#10, True);
  PgDiagnosticSummary := 'Detection: Registry=' +
    BooleanText(DetectionValue(OutputText, 'REGISTRY_DETECTED') = '1') +
    ' | Service=' + BooleanText(DetectionValue(OutputText, 'SERVICE_DETECTED') = '1') +
    ' | Filesystem=' + BooleanText(DetectionValue(OutputText, 'FILESYSTEM_DETECTED') = '1') +
    ' | PATH=' + BooleanText(DetectionValue(OutputText, 'PATH_DETECTED') = '1');
  StringChangeEx(PgDiagnosticSummary, 'True', 'Yes', True);
  StringChangeEx(PgDiagnosticSummary, 'False', 'No', True);
  Log('PATH lookup result: ' + DetectionValue(OutputText, 'PATH_PSQL'));
  Log('Registry lookup result: detected=' + DetectionValue(OutputText, 'REGISTRY_DETECTED') +
    '; view=' + DetectionValue(OutputText, 'REGISTRY_VIEW') +
    '; key=' + DetectionValue(OutputText, 'REGISTRY_KEY') +
    '; base dir=' + DetectionValue(OutputText, 'REGISTRY_BASE_DIR'));
  Log('Service lookup result: detected=' + DetectionValue(OutputText, 'SERVICE_DETECTED') +
    '; name=' + PgServiceName + '; status=' + PgServiceStatus);
  Log('Filesystem lookup result: detected=' + DetectionValue(OutputText, 'FILESYSTEM_DETECTED'));
  Log('Detected PostgreSQL version: ' + DetectionValue(OutputText, 'VERSION') +
    '; major=' + PgMajorVersion);
  Log('Detected PostgreSQL psql: ' + DetectionValue(OutputText, 'PSQL_PATH'));
  Log('Detected PostgreSQL bin directory: ' + PgBinDir);
  Result := DetectionValue(OutputText, 'DETECTED') = '1';
  Log('PostgreSQLDetected=' + BooleanText(Result));
end;

procedure InitializeWizard;
begin
  AppPort := '8080'; DataRoot := ExpandConstant('{commonappdata}\Kovax FlowOps');
  DbChoicePage := CreateCustomPage(wpSelectDir, 'Database setup', 'Choose how to provide PostgreSQL');
  DbIntroLabel := TNewStaticText.Create(DbChoicePage.Surface); DbIntroLabel.Parent := DbChoicePage.Surface; DbIntroLabel.Caption := 'PostgreSQL is used to store FlowOps data.';
  DbInstalledRadio := TNewRadioButton.Create(DbChoicePage.Surface); DbInstalledRadio.Parent := DbChoicePage.Surface; DbInstalledRadio.Caption := 'Use the PostgreSQL installation already on this computer';
  DbAutomaticRadio := TNewRadioButton.Create(DbChoicePage.Surface); DbAutomaticRadio.Parent := DbChoicePage.Surface; DbAutomaticRadio.Caption := 'Install PostgreSQL automatically';
  PostgreSQLDetected := DetectPostgres;
  DbStatusLabel := TNewStaticText.Create(DbChoicePage.Surface); DbStatusLabel.Parent := DbChoicePage.Surface;
  { Temporary development-build diagnostics: remove this appended line when no longer needed. }
  DbStatusLabel.Caption := PgDetectionSummary + #13#10#13#10 + PgDiagnosticSummary;
  if PostgreSQLDetected then DbInstalledRadio.Checked := True else DbAutomaticRadio.Checked := True;
  LayoutDatabasePage;
  DbAdminPage := CreateInputQueryPage(DbChoicePage.ID, 'PostgreSQL administrator', 'Connect to PostgreSQL', 'These credentials are used only to create the FlowOps database and are discarded.');
  DbAdminPage.Add('Host:', False); DbAdminPage.Add('Port:', False); DbAdminPage.Add('Administrator username:', False); DbAdminPage.Add('Administrator password:', True);
  DbAdminPage.Values[0] := 'localhost'; DbAdminPage.Values[1] := '5432'; DbAdminPage.Values[2] := 'postgres';
  LayoutQueryPage(DbAdminPage);
  AdminPage := CreateInputQueryPage(DbAdminPage.ID, 'Create Kovax FlowOps Administrator', 'Set up the first administrator', 'This account will be the first ADMIN account.');
  AdminPage.Add('Full name:', False); AdminPage.Add('Email:', False); AdminPage.Add('Password:', True); AdminPage.Add('Confirm password:', True);
  LayoutQueryPage(AdminPage);
  CreateStartupControls;
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;
  if CurPageID = DbChoicePage.ID then InstallPostgres := DbAutomaticRadio.Checked;
  if CurPageID = AdminPage.ID then begin
    if Trim(AdminPage.Values[0]) = '' then begin MsgBox('Enter the administrator name.', mbError, MB_OK); Result := False; end;
    if Pos('@', AdminPage.Values[1]) < 2 then begin MsgBox('Enter a valid administrator email.', mbError, MB_OK); Result := False; end;
    if Length(AdminPage.Values[2]) < 8 then begin MsgBox('Use a password of at least 8 characters.', mbError, MB_OK); Result := False; end;
    if AdminPage.Values[2] <> AdminPage.Values[3] then begin MsgBox('The passwords do not match.', mbError, MB_OK); Result := False; end;
  end;
end;

function ShouldSkipPage(PageID: Integer): Boolean;
begin
  Result := (PageID = DbAdminPage.ID) and InstallPostgres;
end;

function ConfigureDatabase: Boolean;
var Json: String; Code: Integer; InputPath: String;
begin
  Result := False;
  InputPath := ExpandConstant('{tmp}\flowops-setup.json');
  Json := '{"host":"' + JsonEscape(DbAdminPage.Values[0]) + '","port":"' + JsonEscape(DbAdminPage.Values[1]) + '","adminUser":"' + JsonEscape(DbAdminPage.Values[2]) + '","postgresAdminPassword":"' + JsonEscape(DbAdminPage.Values[3]) + '","postgresBin":"' + JsonEscape(PgBinDir) + '","postgresService":"' + JsonEscape(PgServiceName) + '","postgresStatus":"' + JsonEscape(PgServiceStatus) + '","postgresVersion":"' + JsonEscape(PgMajorVersion) + '","postgresDetection":"' + JsonEscape(PgDetectionMessage) + '","appUser":"kovax_user","database":"kovax_flowops","appPort":"' + JsonEscape(AppPort) + '","adminName":"' + JsonEscape(AdminPage.Values[0]) + '","adminEmail":"' + JsonEscape(AdminPage.Values[1]) + '","adminPassword":"' + JsonEscape(AdminPage.Values[2]) + '"}';
  SaveStringToFile(InputPath, Json, False);
  Exec(PsPath, '-NoProfile -ExecutionPolicy Bypass -File "' + ExpandConstant('{app}\tools\setup-database.ps1') + '" -InputFile "' + InputPath + '" -DataRoot "' + DataRoot + '"', '', SW_HIDE, ewWaitUntilTerminated, Code);
  DeleteFile(InputPath);
  if Code <> 0 then begin MsgBox('Database setup failed. See ' + DataRoot + '\logs\installer-database.log.', mbError, MB_OK); Exit; end;
  Result := True;
end;

function ServiceExists: Boolean;
var Code: Integer;
begin
  Result := Exec(ExpandConstant('{sys}\sc.exe'), 'query KovaxFlowOps', '', SW_HIDE,
    ewWaitUntilTerminated, Code) and (Code = 0);
end;

procedure RemoveExistingService;
var Code, Attempt: Integer; WrapperPath: String;
begin
  if not ServiceExists then
  begin
    Log('Existing KovaxFlowOps service: not found');
    Exit;
  end;

  Log('Existing KovaxFlowOps service found; stopping and uninstalling it before files are replaced');
  WrapperPath := ExpandConstant('{app}\KovaxFlowOps.exe');
  if FileExists(WrapperPath) then
  begin
    Exec(WrapperPath, 'stop', ExpandConstant('{app}'), SW_HIDE, ewWaitUntilTerminated, Code);
    Log('Existing WinSW stop exit code: ' + IntToStr(Code));
    Exec(WrapperPath, 'uninstall', ExpandConstant('{app}'), SW_HIDE, ewWaitUntilTerminated, Code);
    Log('Existing WinSW uninstall exit code: ' + IntToStr(Code));
  end;

  if ServiceExists then
  begin
    Exec(ExpandConstant('{sys}\sc.exe'), 'stop KovaxFlowOps', '', SW_HIDE, ewWaitUntilTerminated, Code);
    Log('Fallback service stop exit code: ' + IntToStr(Code));
    Exec(ExpandConstant('{sys}\sc.exe'), 'delete KovaxFlowOps', '', SW_HIDE, ewWaitUntilTerminated, Code);
    Log('Fallback service delete exit code: ' + IntToStr(Code));
  end;

  for Attempt := 1 to 20 do
  begin
    if not ServiceExists then
    begin
      Log('Existing KovaxFlowOps service removed');
      Exit;
    end;
    Sleep(250);
  end;

  MsgBox('The existing Kovax FlowOps service could not be removed. Close Windows Services and retry the installer.', mbError, MB_OK);
  Abort;
end;

procedure ConfigureServiceDependency;
var Code: Integer;
begin
  if PgServiceName = '' then
  begin
    Log('PostgreSQL service dependency not configured: no service name was detected');
    Exit;
  end;

  Log('Configuring KovaxFlowOps dependency: ' + PgServiceName);
  if (not Exec(ExpandConstant('{sys}\sc.exe'),
    'config KovaxFlowOps depend= ' + PgServiceName, '', SW_HIDE,
    ewWaitUntilTerminated, Code)) or (Code <> 0) then
  begin
    Log('Service dependency configuration failed; exit code: ' + IntToStr(Code));
    MsgBox('Kovax FlowOps could not be configured to depend on ' + PgServiceName + '.', mbError, MB_OK);
    Abort;
  end;
  Log('Service dependency configured successfully: ' + PgServiceName);
end;

procedure InstallPostgresIfNeeded;
  var Code: Integer; PasswordFile: String; Password: AnsiString; PasswordText: String;
begin
  if not InstallPostgres then Exit;
  if not FileExists(ExpandConstant('{tmp}\postgresql-installer.exe')) then begin MsgBox('The PostgreSQL prerequisite is not included in this build.', mbError, MB_OK); Abort; end;
  PasswordFile := ExpandConstant('{tmp}\flowops-postgres-password.txt');
  Exec(PsPath, '-NoProfile -Command "[guid]::NewGuid().ToString(''N'') + [guid]::NewGuid().ToString(''N'')" > "' + PasswordFile + '"', '', SW_HIDE, ewWaitUntilTerminated, Code);
  if not LoadStringFromFile(PasswordFile, Password) then Abort;
  PasswordText := Password;
  StringChangeEx(PasswordText, #13, '', True); StringChangeEx(PasswordText, #10, '', True);
  DbAdminPage.Values[0] := 'localhost'; DbAdminPage.Values[1] := '5432'; DbAdminPage.Values[2] := 'postgres'; DbAdminPage.Values[3] := PasswordText;
  Exec(ExpandConstant('{tmp}\postgresql-installer.exe'), '--mode unattended --superpassword="' + PasswordText + '" --serverport=5432', '', SW_HIDE, ewWaitUntilTerminated, Code);
  DeleteFile(PasswordFile);
  if Code <> 0 then begin MsgBox('PostgreSQL installation failed. The installer will stop without starting FlowOps.', mbError, MB_OK); Abort; end;
  PostgreSQLDetected := DetectPostgres;
  if not PostgreSQLDetected then begin MsgBox('PostgreSQL was installed but its Windows service could not be detected.', mbError, MB_OK); Abort; end;
end;

procedure InstallServiceAndFirewall;
var Code: Integer;
begin
  if (not Exec(ExpandConstant('{app}\KovaxFlowOps.exe'), 'install', ExpandConstant('{app}'), SW_HIDE, ewWaitUntilTerminated, Code)) or (Code <> 0) then
  begin
    MsgBox('The Kovax FlowOps Windows service could not be installed.', mbError, MB_OK);
    Abort;
  end;
  ConfigureServiceDependency;
  if (not Exec(ExpandConstant('{sys}\sc.exe'), 'config KovaxFlowOps start= auto', '', SW_HIDE, ewWaitUntilTerminated, Code)) or (Code <> 0) then
  begin
    MsgBox('The Kovax FlowOps Windows service startup mode could not be configured.', mbError, MB_OK);
    Abort;
  end;
  Exec(ExpandConstant('{sys}\netsh.exe'), 'advfirewall firewall add rule name="Kovax FlowOps" dir=in action=allow protocol=TCP localport=' + AppPort + ' profile=private', '', SW_HIDE, ewWaitUntilTerminated, Code);
  if (not Exec(ExpandConstant('{app}\KovaxFlowOps.exe'), 'start', ExpandConstant('{app}'), SW_HIDE, ewWaitUntilTerminated, Code)) or (Code <> 0) then
  begin
    MsgBox('The Kovax FlowOps service could not be started. See ' + DataRoot + '\logs\KovaxFlowOps.wrapper.log.', mbError, MB_OK);
    Abort;
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssInstall then RemoveExistingService;
  if CurStep = ssPostInstall then begin
    InstallPostgresIfNeeded;
    if not ConfigureDatabase then Abort;
    InstallServiceAndFirewall;
  end;
end;

procedure CurPageChanged(CurPageID: Integer);
begin
  if CurPageID = wpFinished then
  begin
    WizardForm.FinishedHeadingLabel.Visible := False;
    WizardForm.FinishedLabel.Visible := False;
    WizardForm.RunList.Visible := False;
    StartReadinessCheck;
  end;
end;
