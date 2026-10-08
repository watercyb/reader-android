if (typeof window.pageReader === "undefined") {
  const readerInstance = new (class {
    constructor() {
      this.addScrollListener();
    }
    textSegments = [];
    lineBreakers = [];
    nextElementId = 0;
    elementIdOffset = 109138740;
    readingModeHTML = "";
    previousProcessedNode = document.body;
    hasLineBreaker = false;
    highlightedElements = [];
    /*windowHeight needs to be equal to webView.getHeight() / webView.getScaleFactor()*/
    windowHeight = 1; //        windowHeight = target;
    currentSelectionRange = document.body;
    extractionDurationMs = 0;
    newlineTags = new Set([
      "P",
      "DIV",
      "H1",
      "H2",
      "H3",
      "H4",
      "H5",
      "H6",
      "LI",
      "BLOCKQUOTE",
      "HR",
      "PRE",
      "TR",
    ]);
    hasScrollListener = false;
    scrollUpdateTimeout;
    /* new */
    textMutationObservers = new Map();

    processNodeForReading(node) {
      try {
        if (node.nodeType === Node.TEXT_NODE) {
          let text = node.nodeValue.replace("\n", "").trim();
          if (text.length > 0) {
            const textSegment = node.nodeValue
              .replace("\n", "")
              .split(
                /(?<=\. )|(?<=。)|(?<=, )|(?<=，)|(?<=\?)|(?<=？)|(?<=!)|(?<=！)/g,
              );
            if (textSegment.length > 1) {
              let isNewLine = false;
              if (
                this.hasLineBreaker ||
                this.hasLineBreak(this.previousProcessedNode, node)
              ) {
                isNewLine = true;
                this.readingModeHTML += "<br><br>";
                this.hasLineBreaker = false;
              }
              const segmentContainer = document.createElement("span");
              let segmentElement;
              for (let t of textSegment) {
                segmentElement = document.createElement("span");
                segmentElement.setAttribute(
                  "style",
                  "background-color: transparent; color: ; display: inline; word-Break: break-word;",
                );
                segmentElement.setAttribute("id", this.nextElementId++);
                segmentElement.setAttribute("class", "pageReader");
                segmentElement.innerText = t;
                segmentContainer.appendChild(segmentElement);
                if (isNewLine) {
                  this.lineBreakers.push(true);
                  isNewLine = false;
                } else {
                  this.lineBreakers.push(false);
                }
                this.textSegments.push(t.trim());
                this.readingModeHTML += segmentElement.outerHTML;
              }

              node.replaceWith(segmentContainer);
              /* new */
              const observer = new MutationObserver((mutations) => {
                segmentContainer.replaceWith(node);
                segmentContainer.remove();
                observer.disconnect();
                this.sendMessage("mutation");
              });
              observer.observe(node, {
                characterData: true,
              });
              this.textMutationObservers.set(observer, node);
              /* new */
              this.previousProcessedNode = segmentElement;
            } else {
              const range = document.createRange();
              range.selectNode(node);
              const segmentElement = document.createElement("span");
              segmentElement.setAttribute(
                "style",
                "background-color: transparent; color: ; display: inline; word-Break: break-word;",
              );
              segmentElement.setAttribute("id", this.nextElementId++);
              segmentElement.setAttribute("class", "pageReader");
              range.surroundContents(segmentElement);
              if (
                this.hasLineBreaker ||
                this.hasLineBreak(this.previousProcessedNode, node)
              ) {
                this.lineBreakers.push(true);
                this.readingModeHTML += "<br><br>" + node.parentNode.outerHTML;
                this.hasLineBreaker = false;
              } else {
                this.lineBreakers.push(false);
                this.readingModeHTML += node.parentNode.outerHTML;
              }
              this.textSegments.push(text);
              this.previousProcessedNode = node;
            }
          }
        } else if (node.className === "pageReader") {
          let text = node.innerText.replace("\n", "").trim();
          node.id = this.nextElementId++;
          node.style.backgroundColor = "transparent";
          if (
            this.hasLineBreaker ||
            this.hasLineBreak(this.previousProcessedNode, node)
          ) {
            this.lineBreakers.push(true);
            this.readingModeHTML += "<br><br>" + node.outerHTML;
            this.hasLineBreaker = false;
          } else {
            this.lineBreakers.push(false);
            this.readingModeHTML += node.outerHTML;
          }
          this.textSegments.push(text);
          this.previousProcessedNode = node;
        } else if (node.nodeType === Node.ELEMENT_NODE) {
          if (node.tagName === "BR") {
            this.hasLineBreaker = true;
            return;
          }
          if (this.newlineTags.has(node.tagName)) this.hasLineBreaker = true;
          if (this.isVisible(node)) {
            node.childNodes.forEach((child) => {
              this.processNodeForReading(child);
              if (this.newlineTags.has(child.tagName))
                this.hasLineBreaker = true;
            });
          }
        }
      } catch (err) {
        throw new Error("processNodeForReading: " + err.message);
      }
    }
    isVisible(element) {
      try {
        if (element === null || element.tagName === "A") return true;
        const style = window.getComputedStyle(element);
        const rect = element.getBoundingClientRect();

        const visibleStyle =
          style.width !== "0px" &&
          style.height !== "0px" &&
          style.opacity !== "0" &&
          style.display !== "none" &&
          style.visibility !== "hidden" &&
          rect.width !== 0 &&
          rect.height !== 0;

        if (!visibleStyle) return false;

        const docWidth = Math.max(
          document.documentElement.scrollWidth,
          document.body.scrollWidth,
        );
        const docHeight = Math.max(
          document.documentElement.scrollHeight,
          document.body.scrollHeight,
        );

        const elementTop = rect.top + window.scrollY;
        const elementLeft = rect.left + window.scrollX;
        const elementBottom = elementTop + rect.height;
        const elementRight = elementLeft + rect.width;

        const outOfPage =
          elementBottom < 0 ||
          elementRight < 0 ||
          elementTop > docHeight ||
          elementLeft > docWidth;

        return !outOfPage;
      } catch (err) {
        throw new Error("isVisible: " + err.message);
      }
    }
    hasLineBreak(previousNode, nextNode) {
      try {
        if (previousNode === null) return false;
        const range = document.createRange();
        range.setStartAfter(previousNode);
        range.setEndBefore(nextNode);
        return range.toString().includes("\\n");
      } catch (err) {
        throw new Error("hasLineBreak: " + err.message);
      }
    }
    /* Will Be Called From Java */
    addTagToPage() {
      try {
        const start = Date.now();
        /* new */
        for (const [observer, node] of this.textMutationObservers) {
          if (!document.contains(node)) {
            observer.disconnect();
            this.textMutationObservers.delete(observer);
          }
        }
        /* new */
        this.textSegments = [];
        this.lineBreakers = [];
        this.nextElementId = this.elementIdOffset;
        this.readingModeHTML =
          "<title>" +
          document.title +
          '</title><body style="font-size: 16pt;">';
        this.processNodeForReading(document.documentElement);
        this.readingModeHTML += "</body>";
        const end = Date.now();
        this.extractionDurationMs = end - start;
        return this.extractionDurationMs;
      } catch (err) {
        this.sendErrorMessage("addTagToPage: " + err.message);
      }
    }
    addScrollListener() {
      if (!this.hasScrollListener) {
        window.addEventListener("scroll", () => this.delayUpdatePosition());
        this.hasScrollListener = true;
      }
    }
    /* Will Be Called From Java */
    updateHighlightAndScroll(moveDirection, segmentIndexes, initialScrollY) {
      try {
        for (let element of this.highlightedElements) {
          if (element != null) {
            element.style.background = "transparent";
            element.style.color = "";
          }
        }
        this.highlightedElements = [];
        const range = document.createRange();
        let firstElement = null;
        let lastElement = null;
        for (let idx of segmentIndexes) {
          idx += this.elementIdOffset;
          const element = document.getElementById(idx);
          if (element != null) {
            element.style.background = "yellow";
            element.style.color = "black";
            this.highlightedElements.push(element);
            if (firstElement === null) firstElement = element;
            lastElement = element;
          } else {
            this.sendMessage("unmatching");
          }
        }
        if (!firstElement) return 0;
        range.setStartBefore(firstElement);
        range.setEndAfter(lastElement);
        const box = range.getBoundingClientRect();
        if (this.currentSelectionRange === null)
          this.currentSelectionRange = range;
        let res = 0;
        let yPrv =
          this.currentSelectionRange.getBoundingClientRect().y +
          window.scrollY -
          initialScrollY;
        if (moveDirection == -1) {
          if (
            box.y + window.scrollY - initialScrollY <
            this.windowHeight * 0.1
          ) {
            firstElement.scrollIntoView(true);
            window.scrollBy(0, -this.windowHeight * 0.1);
            res = -1;
          }
        } else if (
          (yPrv > 0 && yPrv < this.windowHeight * 1.1) ||
          moveDirection == 1
        ) {
          if (
            box.y + window.scrollY - initialScrollY + box.height >
            this.windowHeight * 0.9
          ) {
            lastElement.scrollIntoView(false);
            window.scrollBy(0, this.windowHeight * 0.1);
            res = 1;
          } else if (box.y + window.scrollY - initialScrollY < 0) {
            firstElement.scrollIntoView(true);
            window.scrollBy(0, -this.windowHeight * 0.1);
            res = -1;
          }
        }
        this.currentSelectionRange = range;
        this.delayUpdatePosition();
        return res;
      } catch (err) {
        this.sendErrorMessage("updateHighlightAndScroll: " + err.message);
      }
    }
    /* Will Be Called From Java */
    clearIdx() {
      try {
        for (let element of this.highlightedElements) {
          if (element != null) {
            element.style.background = "transparent";
            element.style.color = "";
          }
        }
      } catch (err) {
        this.sendErrorMessage("clearIdx: " + err.message);
      }
    }
    /* Will Be Called From Java */
    scrollToHighlightedSegment(behavior) {
      try {
        if (
          this.highlightedElements.length > 0 &&
          this.highlightedElements[this.highlightedElements.length - 1]
        ) {
          let rect =
            this.highlightedElements[
              this.highlightedElements.length - 1
            ].getBoundingClientRect();
          window.scrollTo({
            top:
              rect.bottom +
              window.scrollY -
              window.innerHeight +
              this.windowHeight * 0.1,
            behavior: behavior,
          });
        }
      } catch (err) {
        this.sendErrorMessage("scrollToHighlightedSegment: " + err.message);
      }
    }
    /* Will Be Called From Java */
    getElementByPosition(x, y) {
      try {
        y -= window.scrollY;
        if (
          document.elementFromPoint(x, y) &&
          document.elementFromPoint(x, y).className === "pageReader"
        )
          return (
            document.elementFromPoint(x, y).id - pageReader.elementIdOffset
          );
      } catch (err) {
        this.sendErrorMessage("getElementByPosition: " + err.message);
      }
    }
    /* Will Be Called From Java */
    loadContent() {
      this.addTagToPage();
      return JSON.stringify([this.textSegments, this.lineBreakers]);
    }
    /* Will Be Called From Java */
    isExtractedContentStillPresent() {
      try {
        if (this.textSegments.length == 0) return true;
        let found = 0;
        let missing = 0;
        for (let i = 0; i < this.textSegments.length; i++) {
          if (document.getElementById(i + this.elementIdOffset)) {
            if (++found >= 30) return true;
          } else {
            missing++;
          }
        }
        return found / (found + missing) >= 0.75;
      } catch (err) {
        this.sendErrorMessage("isExtractedContentStillPresent: " + err.message);
      }
    }
    /* Will Be Called From Java */
    updateThemeColor(darkMode) {
      if (darkMode) {
        document.body.style.backgroundColor = "#121212";
        document.body.style.color = "#BDBDBD";
      } else {
        document.body.style.backgroundColor = "white";
        document.body.style.color = "black";
      }
    }
    sendErrorMessage(err) {
      Android.onError(err);
    }
    delayUpdatePosition() {
      try {
        clearTimeout(this.scrollUpdateTimeout);
        this.scrollUpdateTimeout = setTimeout(() => {
          if (pageReader.currentSelectionRange)
            Android.onPageScroll(
              (window.scrollY /
                (document.documentElement.scrollHeight - window.innerHeight)) *
                100,
            );
        }, 200);
      } catch (err) {
        this.sendErrorMessage("delayUpdatePosition: " + err.message);
      }
    }
    sendMessage(message) {
      Android.onMessage(message);
    }
    /* Will Be Called From Java */
    performClick(segmentIndexes) {
      const keywords = ["next", "下一"];
      for (let idx of segmentIndexes) {
        idx += this.elementIdOffset;
        const element = document.getElementById(idx);
        if (element != null) {
          const text = element.innerText.toLowerCase();
          for (let keyword of keywords) {
            if (text.includes(keyword)) {
              this.sendMessage("Next Button found: " + element.innerText);
              element.click();
              return;
            }
          }
        } else {
          this.sendMessage("unmatching");
        }
      }
    }
  })();

  window.pageReader = readerInstance;
}
