#define AppName "FlowOps Client"
#define AppPublisher "FlowOps Contributors"
#define AppId "{E19A26B7-8D54-4D17-9B90-A56A8F82E91B}"

[Setup]
AppId={{#AppId}
AppName={#AppName}
AppVersion=1.0.0
AppPublisher={#AppPublisher}
DefaultDirName={localappdata}\Programs\FlowOps Client
DefaultGroupName=FlowOps
SetupIconFile=FlowOps.ico
UninstallDisplayIcon={app}\FlowOps.ico
DisableDirPage=yes
DisableProgramGroupPage=yes
OutputDir=..\dist\installer
OutputBaseFilename=FlowOps-Client-Setup
PrivilegesRequired=lowest
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
VersionInfoDescription=FlowOps lightweight Windows client installer

[Files]
Source: "..\client\FlowOps-Client.ps1"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\client\FlowOps-Client.ps1"; Flags: dontcopy
Source: "FlowOps.ico"; DestDir: "{app}"; Flags: ignoreversion

[Dirs]
Name: "{localappdata}\FlowOps Client"

[Icons]
Name: "{group}\FlowOps"; Filename: "{sys}\WindowsPowerShell\v1.0\powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File ""{app}\FlowOps-Client.ps1"""; WorkingDir: "{app}"; IconFilename: "{app}\FlowOps.ico"; IconIndex: 0
Name: "{group}\Configure FlowOps Client"; Filename: "{sys}\WindowsPowerShell\v1.0\powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -File ""{app}\FlowOps-Client.ps1"" -Configure"; WorkingDir: "{app}"; IconFilename: "{app}\FlowOps.ico"; IconIndex: 0
Name: "{userdesktop}\FlowOps"; Filename: "{sys}\WindowsPowerShell\v1.0\powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File ""{app}\FlowOps-Client.ps1"""; WorkingDir: "{app}"; IconFilename: "{app}\FlowOps.ico"; IconIndex: 0; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional shortcuts:"; Flags: checkedonce

[UninstallDelete]
Type: files; Name: "{localappdata}\FlowOps Client\server-url.txt"
Type: dirifempty; Name: "{localappdata}\FlowOps Client"

[Code]
var
  ServerPage: TInputQueryWizardPage;
  TestButton: TNewButton;
  TestStatus: TNewStaticText;

function PowerShellPath: String;
begin
  Result := ExpandConstant('{sys}\WindowsPowerShell\v1.0\powershell.exe');
end;

function IsValidServerUrl(const Value: String): Boolean;
var
  Normalized: String;
begin
  Normalized := Lowercase(Trim(Value));
  Result := (((Pos('http://', Normalized) = 1) or (Pos('https://', Normalized) = 1)) and
    (Length(Normalized) > 8) and (Pos(' ', Normalized) = 0) and
    (Pos('"', Normalized) = 0) and (Pos('@', Normalized) = 0));
end;

procedure TestConnectionClick(Sender: TObject);
var
  Code: Integer;
  Parameters: String;
begin
  if not IsValidServerUrl(ServerPage.Values[0]) then
  begin
    TestStatus.Caption := 'Enter a complete HTTP or HTTPS server address first.';
    TestStatus.Font.Color := clRed;
    Exit;
  end;
  TestStatus.Caption := 'Testing connection...';
  TestStatus.Font.Color := clGray;
  WizardForm.Refresh;
  Parameters := '-NoProfile -ExecutionPolicy Bypass -File "' +
    ExpandConstant('{tmp}\FlowOps-Client.ps1') + '" -Test -Url "' +
    Trim(ServerPage.Values[0]) + '"';
  if Exec(PowerShellPath, Parameters, '', SW_HIDE, ewWaitUntilTerminated, Code) and (Code = 0) then
  begin
    TestStatus.Caption := 'Connected to FlowOps.';
    TestStatus.Font.Color := clGreen;
  end
  else
  begin
    TestStatus.Caption := 'Unable to reach FlowOps server. You may correct the address or continue and test later.';
    TestStatus.Font.Color := clRed;
  end;
end;

procedure InitializeWizard;
begin
  ExtractTemporaryFile('FlowOps-Client.ps1');
  ServerPage := CreateInputQueryPage(wpWelcome, 'FlowOps Server Address',
    'Connect this PC to the central FlowOps server',
    'Enter the HTTP or HTTPS address supplied by your company administrator. No login is required during setup.');
  ServerPage.Add('FlowOps Server Address:', False);
  ServerPage.Values[0] := 'http://flowops-server:8080';
  TestButton := TNewButton.Create(ServerPage.Surface);
  TestButton.Parent := ServerPage.Surface;
  TestButton.Caption := 'Test Connection';
  TestButton.Left := 0;
  TestButton.Top := ServerPage.Edits[0].Top + ServerPage.Edits[0].Height + ScaleY(18);
  TestButton.Width := ScaleX(120);
  TestButton.OnClick := @TestConnectionClick;
  TestStatus := TNewStaticText.Create(ServerPage.Surface);
  TestStatus.Parent := ServerPage.Surface;
  TestStatus.Left := 0;
  TestStatus.Top := TestButton.Top + TestButton.Height + ScaleY(12);
  TestStatus.Width := ServerPage.SurfaceWidth;
  TestStatus.Height := ScaleY(46);
  TestStatus.AutoSize := False;
  TestStatus.WordWrap := True;
  TestStatus.Caption := 'The client stores only this server URL.';
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;
  if (CurPageID = ServerPage.ID) and not IsValidServerUrl(ServerPage.Values[0]) then
  begin
    MsgBox('Enter a complete HTTP or HTTPS FlowOps server address, for example http://flowops-server:8080.', mbError, MB_OK);
    Result := False;
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  ConfigPath: String;
begin
  if CurStep = ssPostInstall then
  begin
    ConfigPath := ExpandConstant('{localappdata}\FlowOps Client\server-url.txt');
    if not SaveStringToFile(ConfigPath, Trim(ServerPage.Values[0]), False) then
    begin
      MsgBox('The FlowOps server address could not be saved.', mbError, MB_OK);
      Abort;
    end;
  end;
end;
