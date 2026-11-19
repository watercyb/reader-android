(() => {
    const tracked = new WeakSet();
    let delegated = false; /* click listener on document/window/body */
    const EVENTS = new Set(['click', 'mousedown', 'mouseup', 'pointerdown', 'pointerup', 'touchstart', 'touchend']);
    const keywords = ["next", "下一"];

    const orig = EventTarget.prototype.addEventListener;
    EventTarget.prototype.addEventListener = function (type, fn, opts) {
        if (EVENTS.has(type)) {
            if (this instanceof Element) tracked.add(this);
            else if (this === document || this === window) delegated = true;
        }
        return orig.call(this, type, fn, opts);
    };

    /* el.onclick = fn bypasses addEventListener, so catch the setter too */
    const d = Object.getOwnPropertyDescriptor(HTMLElement.prototype, 'onclick');
    Object.defineProperty(HTMLElement.prototype, 'onclick', {
        get: d.get,
        set(v) { if (v) tracked.add(this); d.set.call(this, v); },
        configurable: true,
    });

    const NATIVE = 'a[href],button,input:not([type=hidden]),select,textarea,summary,[role=button],[role=link],[role=menuitem],[onclick],[tabindex]:not([tabindex="-1"])';

    function visible(el) {
        const r = el.getBoundingClientRect();
        if (r.width < 2 || r.height < 2) return false;
        const cs = getComputedStyle(el);
        if (cs.visibility === 'hidden' || cs.display === 'none' || cs.pointerEvents === 'none') return false;
        const x = r.left + r.width / 2, y = r.top + r.height / 2;
        if (x < 0 || y < 0 || x > innerWidth || y > innerHeight) return false;
        const hit = document.elementFromPoint(x, y);
        return hit && (hit === el || el.contains(hit) || hit.contains(el)); // not covered by an overlay
    }

    window.__findClickables = () => {
        const out = [];
        for (const el of document.querySelectorAll('*')) {
            let reason = null;
            if (tracked.has(el)) reason = 'listener';
            else if (el.matches(NATIVE)) reason = 'semantic';
            else if (getComputedStyle(el).cursor === 'pointer' && el.parentElement &&
                getComputedStyle(el.parentElement).cursor !== 'pointer') reason = 'cursor'; // outermost pointer element only
            if (reason && !el.disabled) {
                const r = el.getBoundingClientRect();
                out.push({
                    reason,
                    tag: el.tagName,
                    text: (el.innerText || el.value || el.getAttribute('aria-label') || '').trim().slice(0, 60),
                    rect: [r.left, r.top, r.width, r.height],
                    element: el,
                });
            }
        }
        return { delegated, items: out };
    };

    window.__clickNextPage = () => {
        const list = window.__findClickables().items;
        for (let item of list) {
            const text = item.text.toLowerCase();
            for (let keyword of keywords) {
                if (text.includes(keyword)) {
                    sendMessage("Next Button found: " + item.text);
                    item.element.click()
                    return;
                }
            }
        }
    }

    function sendErrorMessage(err) {
        Android.onError(err);
    }

    function sendMessage(message) {
        Android.onMessage(message);
    }

    window.__isClickable = (el) => {
        return tracked.has(el) || el.matches(NATIVE) || (getComputedStyle(el).cursor === 'pointer' && el.parentElement &&
            getComputedStyle(el.parentElement).cursor !== 'pointer')
    }
})();