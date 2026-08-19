import { useCallback, useEffect, useMemo, useState } from "react";
import { apiFetch } from "../api/apiFetch";

const tabs=[['ALL','All'],['PENDING_ACTIVATION','Pending Activation'],['ACTIVE','Active'],['DISABLED','Disabled']];
const emptyForm={name:"",email:"",departmentId:"",role:"STAFF",status:"PENDING_ACTIVATION"};
async function errorMessage(response){const data=await response.json().catch(()=>({}));return data.detail||data.message||`Request failed (${response.status})`;}
export default function UserManagementPage({departments}){
  const [users,setUsers]=useState([]),[tab,setTab]=useState('ALL'),[search,setSearch]=useState(''),[loading,setLoading]=useState(true);
  const [editing,setEditing]=useState(null),[form,setForm]=useState(emptyForm),[message,setMessage]=useState(''),[saving,setSaving]=useState(false);
  const load=useCallback(async()=>{setLoading(true);try{const r=await apiFetch('/api/admin/users');if(!r.ok)throw new Error(await errorMessage(r));setUsers(await r.json());}catch(e){setMessage(e.message);}finally{setLoading(false);}},[]);
  // Loading on mount intentionally owns the page's initial request state.
  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(()=>{load();},[load]);
  const filtered=useMemo(()=>users.filter(u=>(tab==='ALL'||u.status===tab)&&(!search||`${u.name} ${u.email}`.toLowerCase().includes(search.toLowerCase()))),[users,tab,search]);
  function add(){setEditing('new');setForm({...emptyForm,departmentId:String(departments[0]?.id||'')});setMessage('');}
  function edit(user){setEditing(user);setForm({name:user.name,email:user.email,departmentId:String(user.departmentId),role:user.role,status:user.status});setMessage('');}
  async function save(event){event.preventDefault();setSaving(true);setMessage('');try{const isNew=editing==='new';const payload={...form,departmentId:Number(form.departmentId)};if(!isNew)delete payload.email;
    const r=await apiFetch(isNew?'/api/admin/users':`/api/admin/users/${editing.id}`,{method:isNew?'POST':'PUT',body:JSON.stringify(payload)});if(!r.ok)throw new Error(await errorMessage(r));const saved=await r.json();
    if(isNew){const link=await apiFetch(`/api/admin/users/${saved.id}/activation-link`,{method:'POST'});if(link.ok){const data=await link.json();await navigator.clipboard.writeText(data.activationLink);setMessage('User created. Activation link copied to clipboard.');}else setMessage('User created. Use Copy Activation Link from the user row.');}
    else setMessage('User updated.');setEditing(null);await load();}catch(e){setMessage(e.message);}finally{setSaving(false);}}
  async function action(user,kind){if(kind==='disable'&&!window.confirm(`Disable ${user.name}'s account?`))return;setMessage('');try{const r=await apiFetch(`/api/admin/users/${user.id}/${kind}`,{method:'POST'});if(!r.ok)throw new Error(await errorMessage(r));setMessage(kind==='disable'?'Account disabled.':'Account reactivated.');await load();}catch(e){setMessage(e.message);}}
  async function copyLink(user){try{const r=await apiFetch(`/api/admin/users/${user.id}/activation-link`,{method:'POST'});if(!r.ok)throw new Error(await errorMessage(r));const data=await r.json();await navigator.clipboard.writeText(data.activationLink);setMessage(`Activation link copied for ${user.name}.`);}catch(e){setMessage(e.message);}}
  return <section className="admin-page"><header className="admin-page-heading"><div><p className="eyebrow">Administration</p><h1>User Management</h1><p>Create company identities and manage access without deleting historical ownership.</p></div><button className="primary-button" onClick={add}>+ Add User</button></header>
    {message&&<div className={`admin-message ${/failed|cannot|invalid|exists/i.test(message)?'error':''}`}>{message}</div>}
    <div className="admin-controls"><div className="admin-tabs">{tabs.map(([id,label])=><button key={id} className={tab===id?'is-active':''} onClick={()=>setTab(id)}>{label}</button>)}</div><input className="admin-search" type="search" placeholder="Search name or email" value={search} onChange={e=>setSearch(e.target.value)}/></div>
    <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Name</th><th>Email</th><th>Department</th><th>Role</th><th>Status</th><th>Created</th><th>Actions</th></tr></thead><tbody>
      {loading?<tr><td colSpan="7">Loading users…</td></tr>:filtered.length===0?<tr><td colSpan="7">No users match this view.</td></tr>:filtered.map(u=><tr key={u.id}><td><strong>{u.name}</strong></td><td>{u.email}</td><td>{u.departmentName}</td><td>{u.role}</td><td><span className={`status-pill ${u.status.toLowerCase()}`}>{u.status.replaceAll('_',' ')}</span></td><td>{new Date(u.createdAt).toLocaleDateString()}</td><td><div className="row-actions"><button onClick={()=>edit(u)}>Edit</button>{u.status==='PENDING_ACTIVATION'&&<button onClick={()=>copyLink(u)}>Copy Activation Link</button>}{u.status==='DISABLED'?<button onClick={()=>action(u,'reactivate')}>Reactivate</button>:<button className="danger-link" onClick={()=>action(u,'disable')}>Disable</button>}</div></td></tr>)}</tbody></table></div>
    {editing&&<div className="modal-backdrop"><div className="modal-card admin-modal"><div className="modal-header"><div><h2>{editing==='new'?'Add User':'Edit User'}</h2><p>{editing==='new'?'The employee will set their own password.':'Changes preserve all historical work.'}</p></div><button className="modal-close-button" onClick={()=>setEditing(null)}>×</button></div><form onSubmit={save} className="admin-form">
      <label>Full Name<input value={form.name} onChange={e=>setForm({...form,name:e.target.value})} required/></label>{editing==='new'&&<label>Email<input type="email" value={form.email} onChange={e=>setForm({...form,email:e.target.value})} required/></label>}
      <label>Department<select value={form.departmentId} onChange={e=>setForm({...form,departmentId:e.target.value})} required>{departments.map(d=><option key={d.id} value={d.id}>{d.name}</option>)}</select></label>
      <label>Role<select value={form.role} onChange={e=>setForm({...form,role:e.target.value})}><option>STAFF</option><option>MANAGER</option><option>ADMIN</option></select></label>
      {editing!=='new'&&<label>Status<select value={form.status} onChange={e=>setForm({...form,status:e.target.value})}><option value="PENDING_ACTIVATION">PENDING ACTIVATION</option>{editing.status!=='PENDING_ACTIVATION'&&<option>ACTIVE</option>}<option>DISABLED</option></select></label>}
      <div className="modal-actions"><button type="button" className="secondary-button" onClick={()=>setEditing(null)}>Cancel</button><button className="primary-button" disabled={saving}>{saving?'Saving…':'Save User'}</button></div></form></div></div>}
  </section>;
}
