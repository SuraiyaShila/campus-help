/* ---------- Sample data (replace with fetch() calls to Spring Boot REST API) ---------- */
const STEPS=['Pending','Accepted','InProgress','Resolved'];
let user={name:'',role:''}, tab='Lost';
let requests=[]; // loaded from the database after login
let lost=[]; // loaded from the database
let resources=[]; // loaded from the database
let notifItems=[],notifSeen=0,notifTimer=null,adminData=null;
let notifs=['Your medical request was accepted by Medical Staff.','Urgent: Campus gate 2 closed until 5 PM.','Your calculator request is pending.'];
/* Registered accounts (demo only). In the real system the Spring Boot backend stores users
   and hashes passwords (e.g. BCrypt); never keep plain-text passwords. */
const accounts=[];

/* ---------- Backend connection ----------
   true  = register/login use the Spring Boot + MySQL backend (it must be running on port 8080)
   false = old demo mode, data lives only in the browser */
const USE_BACKEND=true;
const API='/api'; // same server as the page (Spring Boot serves the frontend), so no CORS problem
async function api(path,body,method){
 let res;
 try{
  res=await fetch(API+path,{method:method||(body?'POST':'GET'),headers:{'Content-Type':'application/json'},body:body?JSON.stringify(body):undefined,credentials:'same-origin'});
 }catch(e){throw new Error('Cannot reach the server. Is the backend running?')}
 const data=await res.json().catch(()=>({}));
 if(res.status===401&&path!=='/login'&&user.id){setTimeout(()=>location.reload(),2000);throw new Error('Your session ended. Please sign in again.')}
 if(!res.ok)throw new Error(data.message||'Something went wrong. Please try again.');
 return data;
}
const users=[['Mohammad Rafi Ahmed','24549010081','Student'],['Suraiya Yeasmin','24549010133','Student'],['Afifa Aziza','24549010095','Student'],['Nazneen Akhter','F-0231','Faculty']];

const NAV={
 common:[['overview','Overview'],['emergency','Emergency help'],['requests','My requests'],['lost','Lost & found'],['resources','Resources'],['notif','Notifications']],
 responder:[['responder','Incoming requests'],['lost','Lost & found'],['notif','Notifications']],
 admin:[['admin','Dashboard'],['responder','All cases'],['lost','Lost & found'],['resources','Resources'],['notif','Notifications']]};

/* ---------- Helpers ---------- */
const $=id=>document.getElementById(id);
const badge=s=>`<span class="badge-st st-${s}">${s==='InProgress'?'In progress':s}</span>`;
function toast(m){$('toastMsg').textContent=m;new bootstrap.Toast($('toast')).show()}

/* ---------- Auth & navigation ---------- */
async function login(){
 const id=$('uid').value.trim();
 if(!id||!$('pw').value){toast('Enter your university ID and password.');return}
 let acc=null;
 if(USE_BACKEND){
  try{const u=await api('/login',{universityId:id,password:$('pw').value});acc={name:u.name,role:u.role}}
  catch(e){toast(e.message);return}
 }else{
  acc=accounts.find(a=>a.id===id);
  if(acc&&acc.pw!==$('pw').value){toast('Wrong password. Please try again.');return}
 }
 user={id:id,name:acc?acc.name:id,role:acc?acc.role:$('role').value};
 $('login').style.display='none';$('app').style.display='block';
 $('who').textContent=user.role+' · '+id;$('hello').textContent=acc?acc.name.split(' ')[0]:user.role;
 const set=user.role==='Administrator'?NAV.admin:['Campus Security','Medical Staff','Hostel Authority'].includes(user.role)?NAV.responder:NAV.common;
 $('nav').innerHTML=set.map(([k,l])=>`<button class="nav-btn" data-k="${k}" onclick="go('${k}')">${l}</button>`).join('');
 $('bottomnav').innerHTML=set.slice(0,5).map(([k,l])=>`<button data-k="${k}" onclick="go('${k}')">${l.split(' ')[0]}</button>`).join('');
 await loadRequests();
 loadNotifs();
 clearInterval(notifTimer);notifTimer=setInterval(loadNotifs,30000);
 go(set[0][0]);
}
async function logout(){try{await api('/logout',{})}catch(e){}location.reload()}

/* ---------- Registration ---------- */
function showRegister(){$('login').style.display='none';$('register').style.display='flex';$('reg-err').classList.add('d-none')}
function showLogin(){$('register').style.display='none';$('login').style.display='flex'}
function regError(m){const e=$('reg-err');e.textContent=m;e.classList.remove('d-none');e.scrollIntoView({block:'nearest'})}
async function register(){
 const f=k=>$('reg-'+k).value.trim();
 const name=f('name'),id=f('id'),email=f('email'),phone=f('phone'),dept=f('dept'),role=$('reg-role').value,pw=$('reg-pw').value,pw2=$('reg-pw2').value;
 if(!name||!id||!email||!phone||!pw)return regError('Please fill in all required fields.');
 if(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))return regError('Enter a valid email address.');
 if(!/^(\+?88)?01[3-9]\d{8}$/.test(phone))return regError('Enter a valid mobile number, like 01712345678.');
 if(pw.length<8)return regError('Password must be at least 8 characters.');
 if(pw!==pw2)return regError('Passwords do not match.');
 if(USE_BACKEND){
  try{await api('/register',{name,universityId:id,email,phone,department:dept,role,password:pw})}
  catch(e){return regError(e.message)}
 }else{
  if(accounts.some(a=>a.id===id))return regError('This university ID is already registered. Try signing in.');
  accounts.push({name,id,email,phone,dept,role,pw});
 }
 users.push([name,id,role]);
 ['name','id','email','phone','dept','pw','pw2'].forEach(k=>$('reg-'+k).value='');
 $('reg-err').classList.add('d-none');
 showLogin();$('uid').value=id;$('role').value=role;$('pw').value='';
 toast('Account created. Please sign in.');
}
function go(k){
 document.querySelectorAll('.view').forEach(v=>v.classList.toggle('show',v.id==='v-'+k));
 document.querySelectorAll('[data-k]').forEach(b=>b.classList.toggle('active',b.dataset.k===k));
 render();
 if(['overview','requests','responder','admin'].includes(k))loadRequests();
 if(['lost','admin'].includes(k))loadLost();
 if(k==='lost'&&REVIEWERS.includes(user.role))loadClaims();
 if(['resources','admin'].includes(k))loadResources();
 if(k==='resources')loadResRequests();
 if(k==='notif')loadNotifs(true);
 if(k==='admin')loadAdmin();
}

/* ---------- Emergency ---------- */
const RESPONDER_ROLES=['Campus Security','Medical Staff','Hostel Authority','Administrator'];
async function loadRequests(){
 if(!user.id)return;
 try{
  const path=RESPONDER_ROLES.includes(user.role)?'/emergency/all?universityId=':'/emergency/mine?universityId=';
  const data=await api(path+encodeURIComponent(user.id));
  requests=data.map(d=>({id:d.id,type:d.type,loc:d.location,urg:d.urgency,desc:d.description,status:d.status,by:d.requesterName}));
  render();
 }catch(e){toast(e.message)}
}
async function addRequest(r){
 try{await api('/emergency',{universityId:user.id,type:r.type,location:r.loc,urgency:r.urg,description:r.desc})}
 catch(e){toast(e.message);return false}
 notifs.unshift('Your '+r.type+' request was submitted.');
 await loadRequests();
 return true;
}
async function sendSOS(){
 let loc='Location not shared';
 try{
  const p=await new Promise((ok,no)=>navigator.geolocation.getCurrentPosition(ok,no,{timeout:5000}));
  loc=p.coords.latitude.toFixed(5)+', '+p.coords.longitude.toFixed(5);
 }catch(e){}
 if(await addRequest({type:'Security',loc,urg:'High',desc:'SOS panic button'})){toast('SOS sent. Responders have your location.');go('requests')}
}
function getLoc(){
 if(!navigator.geolocation){toast('Location is not supported on this device.');return}
 navigator.geolocation.getCurrentPosition(p=>{$('e-loc').value=p.coords.latitude.toFixed(5)+', '+p.coords.longitude.toFixed(5)},()=>toast('Allow location access to use this.'));
}
async function submitEmergency(){
 if(!$('e-loc').value||!$('e-desc').value){toast('Add a location and a short description.');return}
 if(await addRequest({type:$('e-type').value,loc:$('e-loc').value,urg:$('e-urg').value,desc:$('e-desc').value})){
  $('e-desc').value='';toast('Request sent.');go('requests');
 }
}
async function setStatus(id,st){
 try{await api('/emergency/'+id+'/status',{universityId:user.id,status:st},'PATCH')}
 catch(e){toast(e.message)}
 notifs.unshift('Request status changed to '+st+'.');
 await loadRequests();
}

/* ---------- Lost & found ---------- */
function setTab(t){tab=t;['Lost','Found'].forEach(x=>$('tab-'+x).classList.toggle('active',x===t));renderLF()}
const REVIEWERS=['Campus Security','Administrator'];
async function loadLost(){
 if(!user.id)return;
 try{
  const data=await api('/lostfound?universityId='+encodeURIComponent(user.id));
  lost=data.map(d=>({id:d.id,type:d.type,name:d.name,cat:d.category,place:d.place,by:d.reporterName,mine:d.reporterId===user.id,image:d.image||''}));
  renderLF();renderAdmin();
 }catch(e){toast(e.message)}
}
function resizeImage(file,max=800){
 return new Promise((ok,no)=>{
  if(!file.type.startsWith('image/'))return no(new Error('Please choose an image file.'));
  const url=URL.createObjectURL(file),img=new Image();
  img.onload=()=>{
   const k=Math.min(1,max/Math.max(img.width,img.height));
   const c=document.createElement('canvas');c.width=Math.round(img.width*k);c.height=Math.round(img.height*k);
   c.getContext('2d').drawImage(img,0,0,c.width,c.height);
   URL.revokeObjectURL(url);ok(c.toDataURL('image/jpeg',0.7));
  };
  img.onerror=()=>no(new Error('Could not read this image.'));
  img.src=url;
 });
}
async function addLF(){
 if(!$('lf-name').value.trim()){toast('Enter the item name.');return}
 let image='';
 const f=$('lf-photo')&&$('lf-photo').files[0];
 if(f){try{image=await resizeImage(f)}catch(e){toast(e.message);return}}
 try{
  await api('/lostfound',{universityId:user.id,type:$('lf-type').value,name:$('lf-name').value,category:$('lf-c').value,place:$('lf-place').value,image});
  toast('Report submitted.');$('lf-name').value='';$('lf-place').value='';if($('lf-photo'))$('lf-photo').value='';
 }catch(e){toast(e.message);return}
 await loadLost();
}
async function claim(id){
 const item=lost.find(x=>x.id===id);
 const msg=item&&item.type==='Found'
  ?'How can you prove this item is yours? (colour, marks, contents...)'
  :'Where and when did you find this item?';
 const note=prompt(msg);
 if(note===null)return;
 if(!note.trim()){toast('Please write a short note.');return}
 try{await api('/lostfound/'+id+'/claim',{universityId:user.id,note:note.trim()});toast('Sent. Security will review it.')}
 catch(e){toast(e.message)}
}
function ensureClaimBox(){
 if(!$('claimBox'))$('v-lost').insertAdjacentHTML('afterbegin','<div id="claimBox" class="mb-4"></div>');
}
async function loadClaims(){
 ensureClaimBox();
 try{
  const list=await api('/lostfound/claims?universityId='+encodeURIComponent(user.id));
  $('claimBox').innerHTML=list.length?'<h5 class="mb-3">Claims waiting for review</h5>'+list.map(c=>`<div class="card-box mb-2"><strong>${esc(c.claimerName)}</strong> ${c.itemType==='Found'?'says this is theirs:':'says they found:'} <strong>${esc(c.itemName)}</strong>
   <div class="small text-muted mt-1">${esc(c.note)}</div>
   <div class="mt-2 d-flex gap-2"><button class="btn btn-sm btn-green" onclick="reviewClaim(${c.id},'Approved')">Approve</button>
   <button class="btn btn-sm btn-outline-danger" onclick="reviewClaim(${c.id},'Rejected')">Reject</button></div></div>`).join(''):'';
 }catch(e){toast(e.message)}
}
async function reviewClaim(id,status){
 try{await api('/lostfound/claims/'+id,{universityId:user.id,status},'PATCH');toast('Claim '+status.toLowerCase()+'.')}
 catch(e){toast(e.message)}
 await loadClaims();await loadLost();
}

/* ---------- Resources ---------- */
let resIncoming=[],resMine=[];
const RES_BADGE={Pending:'warning',Accepted:'info',Rejected:'danger',Returned:'success'};
async function loadResources(){
 if(!user.id)return;
 try{
  const data=await api('/resources?universityId='+encodeURIComponent(user.id));
  resources=data.map(d=>({id:d.id,name:d.name,place:d.place,status:d.status,by:d.ownerName,mine:d.ownerId===user.id}));
  renderRes();renderAdmin();
 }catch(e){toast(e.message)}
}
function ensureResBox(){
 if(!$('resBox'))$('r-q').insertAdjacentHTML('afterend','<div id="resBox" class="mb-4"></div>');
}
async function loadResRequests(){
 ensureResBox();
 try{
  const q='?universityId='+encodeURIComponent(user.id);
  const both=await Promise.all([api('/resources/requests/incoming'+q),api('/resources/requests/mine'+q)]);
  resIncoming=both[0];resMine=both[1];
  renderResBox();
 }catch(e){toast(e.message)}
}
function renderResBox(){
 const inc=resIncoming.length?'<h5 class="mb-3">Requests for my resources</h5>'+resIncoming.map(r=>`<div class="card-box mb-2"><strong>${esc(r.requesterName)}</strong> wants <strong>${esc(r.resourceName)}</strong>${r.message?`<div class="small text-muted mt-1">${esc(r.message)}</div>`:''}
  <div class="mt-2 d-flex gap-2">${r.status==='Pending'?`<button class="btn btn-sm btn-green" onclick="updateResReq(${r.id},'Accepted')">Accept</button><button class="btn btn-sm btn-outline-danger" onclick="updateResReq(${r.id},'Rejected')">Reject</button>`:`<span class="badge text-bg-info align-self-center">Lent out</span><button class="btn btn-sm btn-outline-success" onclick="updateResReq(${r.id},'Returned')">Mark as returned</button>`}</div></div>`).join(''):'';
 const mine=resMine.length?'<h5 class="mt-4 mb-3">My requests</h5>'+resMine.slice(0,5).map(r=>`<div class="card-box mb-2 d-flex justify-content-between align-items-center"><span><strong>${esc(r.resourceName)}</strong> <span class="text-muted small">from ${esc(r.ownerName)}</span></span><span class="badge text-bg-${RES_BADGE[r.status]||'secondary'}">${esc(r.status)}</span></div>`).join(''):'';
 $('resBox').innerHTML=inc+mine;
}
async function addRes(){
 if(!$('rs-name').value.trim()){toast('Enter the resource name.');return}
 try{
  await api('/resources',{universityId:user.id,name:$('rs-name').value,place:$('rs-owner').value});
  toast('Resource posted.');$('rs-name').value='';$('rs-owner').value='';
 }catch(e){toast(e.message);return}
 await loadResources();
}
async function reqRes(id){
 const msg=prompt('Message to the owner (optional). When do you need it?');
 if(msg===null)return;
 try{await api('/resources/'+id+'/request',{universityId:user.id,message:msg.trim()});toast('Request sent to the owner.')}
 catch(e){toast(e.message);return}
 await loadResRequests();
}
async function updateResReq(id,status){
 try{
  await api('/resources/requests/'+id,{universityId:user.id,status},'PATCH');
  toast(status==='Accepted'?'Request accepted.':status==='Rejected'?'Request rejected.':'Marked as returned.');
 }catch(e){toast(e.message)}
 await loadResRequests();await loadResources();
}

/* ---------- Renderers ---------- */
const esc=t=>String(t??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const reqCard=(r,actions='')=>{
 const n=STEPS.indexOf(r.status);
 return `<div class="card-box mb-3"><div class="d-flex justify-content-between"><div><strong>${esc(r.type)}</strong> · ${esc(r.urg)} urgency${r.by?' · from '+esc(r.by):''}<div class="text-muted small">${esc(r.loc)}</div><div>${esc(r.desc)}</div></div>${badge(r.status)}</div>
 <div class="tl">${STEPS.map((s,i)=>`<span class="${i<=n?'on':''}"></span>`).join('')}</div>${actions}</div>`};
function renderResp(){
 $('respList').innerHTML=requests.map(r=>reqCard(r,`<div class="mt-3 d-flex gap-2">
  <select class="form-select w-auto" onchange="setStatus(${r.id},this.value)">${[...STEPS,'Rejected'].map(s=>`<option ${s===r.status?'selected':''}>${s}</option>`).join('')}</select></div>`)).join('')}
function renderLF(){
 const q=$('lf-q').value.toLowerCase(),c=$('lf-cat').value;
 const list=lost.filter(x=>x.type===tab&&x.name.toLowerCase().includes(q)&&(!c||x.cat===c));
 $('lfList').innerHTML=list.map(x=>`<div class="col-md-6 col-xl-4"><div class="card-box h-100">${x.image&&x.image.startsWith('data:image/')?`<div class="ratio ratio-4x3 mb-2"><img src="${esc(x.image)}" alt="${esc(x.name)}" class="object-fit-cover rounded"></div>`:''}<span class="badge text-bg-light">${esc(x.cat)}</span>
  <h6 class="mt-2">${esc(x.name)}</h6><div class="text-muted small">${esc(x.place)}</div><div class="text-muted small mb-3">Reported by ${esc(x.by)}</div>
  ${x.mine?'<span class="text-muted small">Your report</span>':`<button class="btn btn-sm btn-blue" onclick="claim(${x.id})">${tab==='Lost'?'I found this':'This is mine'}</button>`}</div></div>`).join('')||'<p class="text-muted">No items match. Try another search.</p>'}
function renderRes(){
 const q=$('r-q').value.toLowerCase();
 $('resList').innerHTML=resources.filter(x=>x.name.toLowerCase().includes(q)).map(x=>`<div class="col-md-6 col-xl-4"><div class="card-box h-100">
  <h6>${esc(x.name)}</h6><div class="text-muted small">${esc(x.place)}</div><div class="text-muted small mb-3">Shared by ${esc(x.by)}</div>
  ${x.status!=='Available'?'<span class="badge text-bg-secondary">Lent out</span>':x.mine?'<span class="text-muted small">Your resource</span>':`<button class="btn btn-sm btn-blue" onclick="reqRes(${x.id})">Request item</button>`}</div></div>`).join('')||'<p class="text-muted">No resources found.</p>'}
async function loadAdmin(){
 if(user.role!=='Administrator')return;
 try{adminData=await api('/admin/summary?universityId='+encodeURIComponent(user.id));renderAdmin()}
 catch(e){toast(e.message)}
}
function renderAdmin(){
 const d=adminData||{totalCases:0,activeCases:0,lostItems:0,resourcesLent:0,peak:[],categories:[],users:[]};
 $('sumCards').innerHTML=[['Total cases',d.totalCases],['Active emergencies',d.activeCases],['Lost items',d.lostItems],['Resources lent',d.resourcesLent]]
  .map(([l,n])=>`<div class="col-6 col-lg-3"><div class="card-box"><div class="text-muted small">${l}</div><div class="fs-2 fw-bold">${n}</div></div></div>`).join('');
 const bars=rows=>{
  const max=Math.max(1,...rows.map(r=>r[1]));
  return rows.map(([l,v])=>`<div class="d-flex align-items-center gap-2 mb-2"><div style="width:90px" class="small">${esc(l)}</div><div class="bar" style="width:${Math.round(v/max*70)}%"></div><div class="small">${v}</div></div>`).join('')||'<p class="text-muted small mb-0">No data yet.</p>';
 };
 $('peak').innerHTML=bars(d.peak.map(p=>[p.label,p.count]));
 $('cats').innerHTML=bars(d.categories.map(c=>[c.category,c.count]));
 $('userTbl').innerHTML=d.users.map(u=>`<tr><td>${esc(u.name)}</td><td>${esc(u.universityId)}</td><td>${esc(u.role)}</td></tr>`).join('');
}

/* ---------- Notifications ---------- */
async function loadNotifs(markSeen){
 if(!user.id)return;
 try{notifItems=await api('/notifications?universityId='+encodeURIComponent(user.id))}catch(e){return}
 const key='seen_'+user.id;
 try{notifSeen=Number(localStorage.getItem(key))||0}catch(e){notifSeen=0}
 const shownSince=notifSeen;
 if(markSeen){try{localStorage.setItem(key,String(Date.now()))}catch(e){}}
 const unread=markSeen?0:notifItems.filter(n=>n.time>shownSince).length;
 document.querySelectorAll('[data-k="notif"]').forEach(b=>{b.innerHTML='Notifications'+(unread?` <span class="badge text-bg-danger ms-1">${unread}</span>`:'')});
 notifSeen=shownSince;
 renderNotifs();
}
function renderNotifs(){
 $('notifList').innerHTML=notifItems.length?notifItems.map(n=>`<div class="card-box mb-2 d-flex justify-content-between gap-3"><div>${esc(n.message)}<div class="text-muted small">${new Date(n.time).toLocaleString([],{day:'numeric',month:'short',hour:'numeric',minute:'2-digit'})}</div></div>${n.time>notifSeen?'<span class="badge text-bg-danger align-self-start">New</span>':''}</div>`).join(''):'<p class="text-muted">No notifications yet.</p>';
}
function render(){
 $('recent').innerHTML=requests.slice(0,3).map(r=>`<div class="d-flex justify-content-between border-bottom py-2"><span>${r.type}: ${r.desc}</span>${badge(r.status)}</div>`).join('')||'<p class="text-muted">No requests yet.</p>';
 $('reqList').innerHTML=requests.map(r=>reqCard(r)).join('');
 renderNotifs();
 renderResp();renderLF();renderRes();renderAdmin();
}

/* Photo field for the "Report item" form */
if(!$('lf-photo'))$('lf-place').insertAdjacentHTML('afterend','<label class="form-label small text-muted" for="lf-photo">Photo (optional)</label><input id="lf-photo" type="file" accept="image/*" class="form-control mb-3">');
