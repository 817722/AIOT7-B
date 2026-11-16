(() => {
'use strict';
const main=document.querySelector('#main-content'),form=document.querySelector('#inspection-form');if(!main||!form)return;
const params=new URLSearchParams(location.search), id=params.get('id')||'', edit=main.dataset.editMode==='true'||Boolean(id);
const $=x=>document.getElementById(x), bridge=$('in-bridge-no'),submit=$('in-submit');let recordNo=null;let reviewOpinionUnavailable=false;
const csrf=document.querySelector('meta[name="_csrf"]')?.content, csrfHeader=document.querySelector('meta[name="_csrf_header"]')?.content;
function notice(text,error=true){const m=$('in-form-message');m.textContent=text;m.classList.toggle('is-error',error);m.hidden=false;m.scrollIntoView({block:'nearest'});}
async function request(url, options={}){const headers={Accept:'application/json',...(options.headers||{})};if(options.method&&options.method!=='GET'&&csrf&&csrfHeader)headers[csrfHeader]=csrf;const r=await fetch(url,{credentials:'same-origin',...options,headers});if(!r.ok){let text='';try{const body=await r.json();text=body.message||body.detail||body.error||'';}catch{}throw Error(text||`요청 실패 (${r.status})`);}return r;}
const fields={title:'in-title',inspectionType:'in-type',inspectionDate:'in-date',status:'in-status',inspectorNo:'in-inspector-no',memo:'in-memo',overallResult:'in-overall-result',reviewOpinion:'in-review-opinion'};
async function load(){if(!edit)return;if(!id){notice('수정할 점검 ID가 없습니다. 교량 점검 상세 화면의 기록 수정 버튼을 이용해 주세요.');submit.disabled=true;return;}try{const r=await (await request(`/api/inspections/${encodeURIComponent(id)}`)).json();recordNo=r.no;reviewOpinionUnavailable=!Object.prototype.hasOwnProperty.call(r,'reviewOpinion');bridge.disabled=false;bridge.value=String(r.bridgeNo);if(!bridge.value){const opt=document.createElement('option');opt.value=String(r.bridgeNo);opt.textContent=`${r.bridgeName||'교량'} (#${r.bridgeNo})`;bridge.append(opt);bridge.value=opt.value;}bridge.disabled=true;document.querySelector('.in-hero h1').textContent='점검 기록 수정';submit.textContent='변경사항 저장';for(const [key,elm] of Object.entries(fields)){if($(elm))$(elm).value=r[key]??'';}if(reviewOpinionUnavailable)notice('현재 점검 조회 API가 검토 의견을 반환하지 않습니다. 수정 시 기존 검토 의견이 덮어써질 수 있으므로 저장 전에 확인해 주세요.',true);if(!bridge.value||bridge.selectedIndex<0)throw Error('수정 대상 교량이 선택 목록에 없습니다.');}catch(e){notice(e.message);submit.disabled=true;}}
// Browser file inputs replace their selection on every browse. Keep a separate pending-photo queue.
const pendingPhotos=new Map();
function photoKey(file){return `${file.name}\u0000${file.size}\u0000${file.lastModified}\u0000${file.type}`;}
function preview(){
 const output=$('in-video-preview-list');output.replaceChildren();
 const photoFiles=[...pendingPhotos.entries()];
 const videos=[...$('in-video-single').files,...$('in-video-multiple').files];
 if(!photoFiles.length&&!videos.length){output.textContent='선택된 파일이 없습니다.';return;}
 for(const [key,file] of photoFiles){
  const item=document.createElement('span');item.className='in-file-item in-file-item-removable';
  const name=document.createElement('span');name.textContent=`사진 · ${file.name} (${(file.size/1048576).toFixed(1)} MB)`;
  const remove=document.createElement('button');remove.type='button';remove.className='in-queued-remove';remove.textContent='제외';remove.setAttribute('aria-label',`${file.name} 업로드 대기 목록에서 제외`);
  remove.addEventListener('click',()=>{pendingPhotos.delete(key);preview();});
  item.append(name,remove);output.append(item);
 }
 for(const file of videos){const span=document.createElement('span');span.className='in-file-item';span.textContent=`영상 · ${file.name} (${(file.size/1048576).toFixed(1)} MB)`;output.append(span);}
}
$('in-photo-files').addEventListener('change',event=>{
 for(const file of event.target.files){
  if(!file.type.startsWith('image/')){notice(`${file.name}: 이미지 파일만 선택할 수 있습니다.`);continue;}
  pendingPhotos.set(photoKey(file),file);
 }
 // Reset to allow selecting the same photo(s) again without losing previously selected items.
 event.target.value='';preview();
});
for(const key of ['in-video-single','in-video-multiple'])$(key).addEventListener('change',preview);
async function uploadPhotos(no){
 for(const file of pendingPhotos.values()){
  const fd=new FormData();fd.append('file',file);
  await request(`/api/media/INSPECTION/${no}`,{method:'POST',body:fd});
  pendingPhotos.delete(photoKey(file));
 }
}
async function uploadVideos(no){const files=[...$('in-video-single').files,...$('in-video-multiple').files];if(!files.length)return;const fd=new FormData();for(const file of files)fd.append('files',file);await request(`/api/inspections/videos/${no}`,{method:'POST',body:fd});}
form.addEventListener('submit',async e=>{e.preventDefault();const no=bridge.value||main.dataset.bridgeNo;if(!no){notice('점검 대상 교량을 선택해 주세요.');return;}const payload={bridgeNo:+no};for(const [key,elm] of Object.entries(fields))payload[key]=$(elm)?.value.trim()||'';if(!payload.title||!payload.inspectionType||!payload.inspectionDate||!payload.inspectorNo){notice('점검 기록명, 유형, 일자, 담당자는 필수입니다.');return;}payload.inspectorNo=+payload.inspectorNo;const dateInput=$('in-date');if(!dateInput.checkValidity()){notice('점검 일자는 1900-01-01부터 오늘(한국 시간)까지 입력해 주세요.');dateInput.focus();return;}if(edit&&reviewOpinionUnavailable&&!confirm('조회 API에 기존 검토 의견이 포함되지 않습니다. 저장하면 기존 검토 의견을 덮어쓸 수 있습니다. 계속하시겠습니까?'))return;submit.disabled=true;notice('기록을 저장하고 있습니다.',false);let saved=false;try{let savedId=id;if(edit){await request(`/api/inspections/${encodeURIComponent(id)}`,{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});saved=true;await uploadPhotos(recordNo);await uploadVideos(recordNo);}else{const fd=new FormData(form);for(const [k,v] of Object.entries(payload))fd.set(k,v);const response=await request('/api/inspections',{method:'POST',body:fd});savedId=(await response.text()).replace(/^"|"$/g,'').trim();if(!savedId)throw Error('저장 응답에서 점검 ID가 누락되었습니다.');saved=true;const data=await (await request(`/api/inspections/${encodeURIComponent(savedId)}`)).json();await uploadPhotos(data.no);}notice('저장 완료. 상세 화면으로 이동합니다.',false);location.assign(`/inspections/${encodeURIComponent(no)}?inspectionId=${encodeURIComponent(savedId)}`);}catch(error){notice(`${saved?'점검 기록은 저장되었지만 후속 처리에 실패했습니다':'점검 기록 저장에 실패했습니다'}: ${error.message}`);submit.disabled=false;}});
const dateInput=$('in-date');if(dateInput){const parts=new Intl.DateTimeFormat('en-US',{timeZone:'Asia/Seoul',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(new Date());const part=k=>parts.find(p=>p.type===k)?.value;dateInput.min='1900-01-01';dateInput.max=`${part('year')}-${part('month')}-${part('day')}`;}
load();
})();
