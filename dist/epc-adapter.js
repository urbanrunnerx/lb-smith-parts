/* Snap-on visible catalog adapter. Reads rendered controls only; no account tokens or private APIs. */
function lbEpc(command, request) {
 'use strict';
 request = request || {};
 const clean = value => String(value || '').replace(/[\u200B-\u200D\uFEFF]/g, '').replace(/\s+/g, ' ').trim();
 const text = el => clean(el && el.innerText);
 const visible = el => !!el && el.getClientRects().length > 0 && getComputedStyle(el).visibility !== 'hidden' && !el.closest('[aria-hidden="true"]');
 const all = (selector, root = document) => [...root.querySelectorAll(selector)].filter(visible);
 const hash = value => { let n = 2166136261; for (const ch of value) { n ^= ch.charCodeAt(0); n = Math.imul(n, 16777619); } return (n >>> 0).toString(36); };
 const button = label => all('button').find(b => text(b) === label && !b.disabled);
 const vinNode=document.querySelector('#toolbar-vin-url-anchor');
 const currentVin = visible(vinNode)?text(vinNode).toUpperCase():'';
 const filter = document.querySelector('[title="Toggle VIN filters on and off"] input[type="checkbox"]');
 const ready = /^[A-HJ-NPR-Z0-9]{17}$/.test(request.vin || '') && currentVin === request.vin && !!filter?.checked;
 const signedIn = !!button('Find VIN') || !!currentVin;
 const login = all('input[type="password"]').length > 0;
 const busy = all('[aria-busy="true"],.ag-overlay-loading-center,.p-progress-spinner,.p-progressspinner').length > 0 || all('div,span,p').some(e => e.childElementCount === 0 && /^Loading(?:\.{3}|…)?$/i.test(text(e)));
 const targets = new Map(), scrollTargets = [];
 const crumbNodes = all('ul.breadcrumb [role="menuitem"] > a.p-menuitem-link[href="#"]');
 const breadcrumbs = crumbNodes.map((el, i) => {
  const label = text(el), id = 'crumb-' + i + '-' + hash(label);
  const disabled = el.classList.contains('p-breadcrumb-disabled') || el.getAttribute('aria-disabled') === 'true';
  if (!disabled) targets.set(id, el);
  return {id, label, disabled};
 });
 const context = breadcrumbs.map(b => b.label).join(' > ').slice(0,2000);
 const vehicleHint = all('[title]').find(e => /^ENG:/.test(e.getAttribute('title') || ''));
 const vehicle = clean(vehicleHint?.getAttribute('title') || '');
 const grids = all('[role="grid"]');
 const headings = grid => all('[role="columnheader"]', grid).map(h => ({col:h.getAttribute('col-id'), label:text(h).replace(/[\uF000-\uF8FF]/g, '').trim()}));
 // AG Grid can split one row into pinned and center cells. Merge only matching rendered row IDs.
 const rows = grid => {
  const out = new Map(); let ordinal = 0;
  for (const el of all('[role="row"]', grid)) {
   const cells = all('[role="gridcell"]', el); if (!cells.length) continue;
   const key = el.getAttribute('row-id') || el.getAttribute('aria-rowindex') || 'fixture-' + ordinal++;
   let row = out.get(key); if (!row) { row = {key, cells:new Map(), selected:false}; out.set(key,row); }
   row.selected ||= el.getAttribute('aria-selected') === 'true';
   cells.forEach((c,i) => row.cells.set(c.getAttribute('col-id') || 'column-' + i, c));
  }
  return [...out.values()];
 };
 const cell = (row, col, label, headers) => row.cells.get(col) || row.cells.get('column-' + headers.findIndex(h => h.label === label));
 const value = (row, col, label, headers) => text(cell(row,col,label,headers));
 const addScroll = (grid, groupId) => {
  const e = all('.ag-body-viewport',grid).find(e => e.clientHeight > 0 && e.scrollHeight > e.clientHeight + 2);
  if(e) scrollTargets.push({element:e, groupId, more:e.scrollTop + e.clientHeight < e.scrollHeight - 2});
 };
 const groups = [], parts = []; let partGrid = null, groupCount = 0;
 for (let gi = 0; gi < grids.length; gi++) {
  const grid = grids[gi], headers = headings(grid), has = label => headers.some(h => h.label === label);
  if (has('Call/Base') && has('Part Number')) { partGrid = grid; continue; }
  // Only catalog navigation tables are actionable. Picklist/order/price tables are never targets.
  const locationGrid = has('Base Number') && has('Part Location');
  const illustrationGrid = has('Illustration');
  const groupGrid = has('Group') && headers.every(h => !h.label || h.label === 'Group');
  if (!locationGrid && !illustrationGrid && !groupGrid) continue;
  const id = 'group-' + gi, label = locationGrid ? 'Part location' : illustrationGrid ? 'Illustration' : (++groupCount === 1 ? 'System' : groupCount === 2 ? 'Section' : 'Group ' + groupCount);
  const options = []; let inheritedBase = '', inheritedDescription = '';
  for (const row of rows(grid)) {
   let target, detail = '';
   if (locationGrid) {
    const base = value(row,'crossCatKey','Base Number',headers);
    if(base) { inheritedBase=base; inheritedDescription=value(row,'partDescription','Part Description',headers); }
    if(inheritedBase !== request.base) continue;
    target = cell(row,'partLocation','Part Location',headers); detail=inheritedDescription;
   } else target = cell(row,'name',illustrationGrid?'Illustration':'Group',headers);
   const name = text(target); if(!name) continue;
   const optionId = id + '-' + hash(row.key + '|' + name) + '-' + options.length;
   targets.set(optionId,target); options.push({id:optionId,label:name,detail,selected:row.selected});
  }
  if(options.length) groups.push({id,label,options});
  addScroll(grid,id);
 }
 const thumbs = all('a.thumbnailNonIllustrated[title],a.thumbnail[title]');
 if (thumbs.length) {
  const id = 'thumbnails';
  const options = thumbs.map((el,i) => {
   const label=clean(el.getAttribute('title') || text(el)), optionId=id+'-'+i+'-'+hash(label);
   targets.set(optionId,el); return {id:optionId,label};
  });
  groups.push({id,label:thumbs.some(t=>t.classList.contains('thumbnail'))?'Illustration':'Catalog selection',options});
 }
 if (partGrid && ready) {
  const headers=headings(partGrid); let base='',description='',application='',remarks='',from='',to='',quantity='';
  for(const row of rows(partGrid)) {
   const rowBase=value(row,'calloutLabel','Call/Base',headers);
   if(rowBase) {
    base=rowBase; description=value(row,'renderedDescription','Part Description',headers);
    application=value(row,'APPLICATION','Application',headers); remarks=value(row,'remarks','Restrictions/Remarks',headers);
    from=value(row,'FROM','From',headers);to=value(row,'TO','To',headers);quantity=value(row,'original_qty','Qty',headers);
   }
   const number=value(row,'formattedPartNumber','Part Number',headers);
   if(base!==request.base || !/^([A-Z0-9]{4}-[0-9][A-Z0-9]{3,7}-[A-Z0-9]{1,10}|[A-Z]{1,6}-[A-Z0-9]{1,15})$/.test(number))continue;
   const conventional=number.match(/^[A-Z0-9]{4}-([0-9][A-Z0-9]{3,7})-/);if(conventional&&conventional[1]!==request.base)continue;
   const p={vin:currentVin,base,serviceNumber:number,description,application:value(row,'APPLICATION','Application',headers)||application,
    remarks:value(row,'remarks','Restrictions/Remarks',headers)||remarks,from:value(row,'FROM','From',headers)||from,
    to:value(row,'TO','To',headers)||to,quantity:value(row,'original_qty','Qty',headers)||quantity,context};
   const identity=JSON.stringify(p);if(parts.some(x=>x.id==='part-'+hash(identity)))continue;
   parts.push({id:'part-'+hash(identity),...p});
  }
  addScroll(partGrid,'parts');
 }
 // A visible modal is never silently skipped, including expired-session and equipment questions.
 const modal = all('[role="dialog"],[role="alertdialog"]').find(e=>text(e));
 let stage=busy?'loading':login?'login':!signedIn||modal?'unsupported':!ready?(currentVin===request.vin?'filters':'vehicle'):parts.length?'parts':groups.length?'choices':partGrid?'empty':'unsupported';
 const safeParts = stage==='parts'?parts.slice(0,150):[];
 const safeGroups = stage==='choices'||stage==='parts'?groups:[];
 const messages={login:'Sign into your Snap-on account, then return to guided lookup.',vehicle:'Load the job VIN to start the catalog lookup.',filters:'Turn on VIN filters in the catalog before continuing.',loading:'Waiting for the catalog…',choices:'Choose a catalog location or application below.',parts:'Review the application and select the required service part.',empty:'No matching service number is displayed for this base at this location.',unsupported:modal?'The catalog needs attention: '+text(modal).slice(0,400):'This catalog screen needs the original catalog view. Open Catalog / sign in, then return here.'};
 const canMore=scrollTargets.some(s=>s.more) && ready && !busy && !modal;
 const payload={schema:3,stage,vin:currentVin,ready,signedIn,busy,vehicle,context,message:messages[stage],breadcrumbs:ready?breadcrumbs:[],groups:safeGroups,parts:safeParts,canMore,
  scope:'Currently rendered catalog rows. More rows may be available; choices do not establish fitment.'};
 const snapshotId='snapshot-'+hash(JSON.stringify({...payload,requestedBase:request.base,requestedVin:request.vin}));
 payload.snapshotId=snapshotId;
 if(command==='snapshot')return payload;
 if(command==='state')return {vin:currentVin,ready,signedIn,busy,context,snapshotId};
 if(command==='capture')return ready&&safeParts.length?{vin:currentVin,parts:safeParts,context,snapshotId}:{error:messages[stage]||'Open the correct illustration first.'};
 const setInput=(selector,v)=>{const el=document.querySelector(selector);if(!visible(el))return false;el.focus();Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(el,v);el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));el.blur();return true;};
 if(command==='vin') {
  if(!/^[A-HJ-NPR-Z0-9]{17}$/.test(request.vin||''))return {error:'Enter a complete VIN.'};
  if(busy||modal)return {error:messages[stage]};
  if(!button('Find VIN')||!setInput('#equipmentEntryInputId',request.vin))return {error:'Sign into the Ford catalog, then return to guided lookup.'};
  button('Find VIN').click();return {message:'Loading the requested vehicle…'};
 }
 if(!ready)return {error:'The requested VIN must be active with VIN filters enabled.'};
 if(busy||modal)return {error:messages[stage]};
 if(command==='search') {
  if(!/^\d[A-Z0-9]{3,7}$/.test(request.base||''))return {error:'Invalid base number.'};
  if(!button('Search')||!setInput('#partEntryInputId',request.base))return {error:'Search controls could not be found. Open Catalog / sign in.'};
  button('Search').click();return {message:'Finding locations for this base…'};
 }
 if(request.snapshotId!==snapshotId)return {error:'The catalog changed. Refresh the choices and select again.'};
 if(command==='select') {
  const target=targets.get(request.optionId);
  if(!target||!visible(target))return {error:'This catalog choice is no longer available. Refresh the choices.'};
  target.click();return {message:'Loading the selected catalog location…'};
 }
 if(command==='more') {
  const next=scrollTargets.find(s=>s.more&&(!request.groupId||s.groupId===request.groupId));
  if(!next)return {error:'No additional rendered rows are available here.'};
  next.element.scrollTop=Math.min(next.element.scrollHeight-next.element.clientHeight,next.element.scrollTop+Math.max(80,next.element.clientHeight-55));
  next.element.dispatchEvent(new Event('scroll',{bubbles:true}));return {message:'Loading the next catalog rows…'};
 }
 if(command==='verify') {
  const part=safeParts.find(p=>p.id===request.partId);
  if(!part)return {error:'The selected part or application changed. Refresh and review it again.'};
  return {part:{...part}};
 }
 return {error:'Unsupported catalog action.'};
}
