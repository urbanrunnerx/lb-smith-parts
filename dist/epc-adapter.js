/* Visible-page adapter for an employee's own EPC session. No API, cookies or passwords are read. */
function lbEpc(command,request){
 const visible=e=>!!e&&e.getClientRects().length>0&&getComputedStyle(e).visibility!=='hidden';
 const text=e=>e?.innerText?.trim()||'';
 const vinNode=document.querySelector('#toolbar-vin-url-anchor');
 const currentVin=visible(vinNode)?text(vinNode).toUpperCase():'';
 const filter=document.querySelector('[title="Toggle VIN filters on and off"] input[type="checkbox"]');
 const vinReady=currentVin===request.vin&&!!filter?.checked;
 const buttons=[...document.querySelectorAll('button')];
 const button=label=>buttons.find(b=>visible(b)&&text(b)===label&&!b.disabled);
 const setInput=(selector,value)=>{const input=document.querySelector(selector);if(!visible(input))return false;input.focus();Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(input,value);input.dispatchEvent(new Event('input',{bubbles:true}));input.dispatchEvent(new Event('change',{bubbles:true}));input.blur();return true;};
 if(command==='vin'){
  if(!/^[A-HJ-NPR-Z0-9]{17}$/.test(request.vin))return {error:'Invalid VIN.'};
  if(!button('Find VIN')||!setInput('#equipmentEntryInputId',request.vin))return {error:'Sign in to the Ford catalog, then tap Load VIN.'};
  button('Find VIN').click();return {message:'Loading VIN. Wait for the vehicle to appear before searching.'};
 }
 if(command==='search'){
  if(!vinReady)return {error:currentVin===request.vin?'Turn on EPC VIN filters before searching.':'Load the requested VIN first. The current EPC vehicle does not match.'};
  if(!/^\d[A-Z0-9]{3,7}$/.test(request.base))return {error:'Invalid base number.'};
  if(!button('Search')||!setInput('#partEntryInputId',request.base))return {error:'Search controls were not found. Use the EPC search box manually.'};
  button('Search').click();return {message:'Choose the required location or position in EPC, then tap Review parts.'};
 }
 if(command==='capture'){
  if(!vinReady)return {error:'The requested VIN must be active with VIN filters enabled before parts can be returned.'};
  const groups=[...document.querySelectorAll('[role="grid"]')];
  const grid=groups.find(g=>visible(g)&&[...g.querySelectorAll('[role="columnheader"]')].some(h=>text(h)==='Call/Base'));
  if(!grid)return {error:'Open a specific front/rear or component illustration with a parts list, then tap Review parts.'};
  const headers=[...grid.querySelectorAll('[role="columnheader"]')].map(h=>text(h).replace(/[^A-Za-z/ ]/g,'').trim());
  let currentBase='',description='';const parts=[];
  for(const row of grid.querySelectorAll('[role="row"]')){
   if(!visible(row))continue;const cells=[...row.querySelectorAll('[role="gridcell"]')];if(!cells.length)continue;
   const value=(col,label)=>text(cells.find(c=>c.getAttribute('col-id')===col)||cells[headers.indexOf(label)]);
   const base=value('calloutLabel','Call/Base');if(base){currentBase=base;description=value('renderedDescription','Part Description');}
   const number=value('formattedPartNumber','Part Number');
   if(currentBase!==request.base||!number||!/^([A-Z0-9]{4}-[0-9][A-Z0-9]{3,7}-[A-Z0-9]{1,10}|[A-Z]{1,6}-[A-Z0-9]{1,15})$/.test(number))continue;
   const conventional=number.match(/^[A-Z0-9]{4}-([0-9][A-Z0-9]{3,7})-/);if(conventional&&conventional[1]!==request.base)continue;
   if(parts.some(p=>p.serviceNumber===number))continue;
   parts.push({vin:currentVin,base:request.base,serviceNumber:number,description,remarks:cells.map(text).filter(Boolean).join(' · ').slice(0,1600)});
  }
  const path=[...document.querySelectorAll('[role="menuitem"]')].map(text).filter(s=>/Front|Rear|Knuckle|Hub|Chassis|Powertrain|Body|Electrical|;\s*\d/.test(s)).join(' > ').slice(0,1000);
  return parts.length?{vin:currentVin,parts:parts.slice(0,80),context:path}:{error:'No visible service numbers for this base. Open its illustration and bring the required rows into view.'};
 }
 return {vin:currentVin,ready:vinReady,signedIn:!!button('Find VIN')};
}
