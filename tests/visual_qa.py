"""Real Chromium smoke tests and screenshots for desktop + Android-sized browsers."""
import asyncio
from pathlib import Path
from playwright.async_api import async_playwright
ROOT=Path(__file__).resolve().parents[1]
URL=(Path(__file__).resolve().parents[1]/'index.html').as_uri()
async def run():
    async with async_playwright() as p:
        browser=await p.chromium.launch(headless=True,executable_path='/usr/bin/chromium',args=['--no-sandbox','--disable-dev-shm-usage','--allow-file-access-from-files'])
        for label,width,height,mobile in [('desktop',1440,900,False),('android_landscape',915,412,True),('android_portrait',393,873,True)]:
            ctx=await browser.new_context(viewport={'width':width,'height':height},is_mobile=mobile,has_touch=mobile,device_scale_factor=1)
            page=await ctx.new_page();errors=[]
            page.on('pageerror',lambda e:errors.append(str(e)))
            html=(ROOT/'index.html').read_text()
            import re
            html=re.sub(r'<link rel="stylesheet"[^>]*>', '', html)
            html=re.sub(r'<script type="module"[^>]*></script>', '', html)
            await page.set_content(html,wait_until='domcontentloaded')
            await page.add_style_tag(content=(ROOT/'src/styles.css').read_text())
            engine=(ROOT/'src/engine.mjs').read_text()
            engine=engine[:engine.rfind('export {')]
            app=(ROOT/'src/app.mjs').read_text().split('\n',1)[1]
            await page.add_script_tag(content=engine+'\n'+app)
            await page.screenshot(path=str(ROOT/'tests'/'screenshots'/f'qa_{label}.png'),full_page=True)
            info=await page.evaluate('''() => ({counts:[window.__buraco.getState().hands.map(x=>x.length),window.__buraco.getState().stock.length], phase:document.querySelector('#phaseBadge').innerText, hand:document.querySelectorAll('.hand-card').length, enabled:!document.querySelector('#drawBtn').disabled, scrollX:document.documentElement.scrollWidth, clientX:document.documentElement.clientWidth, scrollY:document.documentElement.scrollHeight, clientY:document.documentElement.clientHeight})''')
            assert info['hand']==11 and info['enabled'] and info['counts'][1]==37,info
            assert info['scrollX']<=info['clientX']+2,(label,info)
            if label=='desktop':
                await page.locator('#drawBtn').click();assert await page.evaluate('window.__buraco.getState().phase')=='play'
                await page.locator('.hand-card').first.click()
                assert await page.locator('#discardBtn').is_enabled()
                await page.locator('#discardBtn').click()
                await page.wait_for_timeout(2600)
                assert await page.evaluate('window.__buraco.getState().current')==0
                await page.locator('#settingsBtn').click()
                await page.locator('#botDifficulty').select_option('especialista')
                await page.locator('#settingsSave').click()
                assert await page.evaluate('window.__buraco.getState().difficulty')=='especialista'
            assert not errors,(label,errors)
            print(label,'PASS',info)
            await ctx.close()
        await browser.close()
if __name__=='__main__':asyncio.run(run())
