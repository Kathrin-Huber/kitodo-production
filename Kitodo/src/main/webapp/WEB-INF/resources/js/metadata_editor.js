/**
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */
/* globals select, setGalleryViewMode, destruct, initialize, scrollToSelectedThumbnail, changeToMapView, PF, scrollToStructureThumbnail,
    scrollToPreviewThumbnail, expandMetadata, preserveMetadata, setConfirmUnload */

var metadataEditor = {
    dragging: false,
    handleMouseDown(event) {
        $(document.activeElement).blur();
        let target = $(event.target);
        if (target.closest(".stripe").length === 1) {
            this.stripes.handleMouseDown(event);
        } else if (target.closest(".thumbnail-container").length === 1) {
            this.pages.handleMouseDown(event, target.closest(".thumbnail-container"));
        }
    },
    handleMouseUp(event) {
        let target = $(event.target);
        if (target.closest(".thumbnail-container").length === 1) {
            this.pages.handleMouseUp(event, target.closest(".thumbnail-container"));
        }
    },
    handleDragStart(event) {
        this.pages.handleDragStart(event);
    },
    pages: {
        handleMouseDown(event, target) {
            if (target.closest(".thumbnail-parent").find(".selected").length === 0) {
                this.select(event, target);
            }
        },
        handleMouseUp(event, target) {
            metadataEditor.dragdrop.removeDragAmountIcon();
            if (metadataEditor.dragging) {
                metadataEditor.dragging = false;
            } else if (event.button !== 2 || target.closest(".thumbnail-parent").find(".selected").length === 0) {
                this.select(event, target);
            }
        },
        handleDragStart(event) {
            metadataEditor.dragging = true;
            metadataEditor.dragdrop.addDragAmountIcon(event);
        },
        select(event, target) {
            if (event.metaKey || event.ctrlKey) {
                metadataEditor.select(target[0].dataset.order, target[0].dataset.stripe, "multi");
            } else if (event.shiftKey) {
                metadataEditor.select(target[0].dataset.order, target[0].dataset.stripe, "range");
            } else {
                metadataEditor.select(target[0].dataset.order, target[0].dataset.stripe, "default");
            }
        }
    },
    stripes: {
        handleMouseDown(event) {
            if (!$(event.target).hasClass("selected")) {
                metadataEditor.select(null, event.target.dataset.stripe, "default");
            }
        },
    },
    select(pageIndex, stripeIndex, selectionType) {
        // call the remoteCommand in gallery.xhtml
        select([
            {name: "page", value: pageIndex},
            {name: "stripe", value: stripeIndex},
            {name: "selectionType", value: selectionType}
        ]);
    }
};

metadataEditor.contextMenu = {
    listen() {
        document.oncontextmenu = function() {
            return false;
        };
        $(document).on("mousedown.thumbnail", ".thumbnail-parent", function(event) {
            if (event.originalEvent.button === 2) {
                PF("mediaContextMenu").show(event);
            }
        });
        $(document).on("mousedown.stripe", ".stripe", function(event) {
            if (event.originalEvent.button === 2) {
                PF("stripeContextMenu").show(event);
            }
        });
    }
};

metadataEditor.dragdrop = {
    addDragAmountIcon(event) {
        var dragAmount = document.querySelectorAll(".thumbnail.selected").length;
        if (dragAmount > 1) {
            var element = document.createElement("div");
            element.id = "dragAmount";
            element.innerText = dragAmount;
            event.target.appendChild(element);
        }
    },
    removeDragAmountIcon() {
        var element = document.getElementById("dragAmount");
        if (element !== null) {
            element.parentNode.removeChild(element);
        }
    }
};

metadataEditor.shortcuts = {
    getGalleryViewMode() {
        return $("#imagePreviewForm\\:galleryViewMode ").text().toUpperCase();
    },
    changeView(galleryViewMode) {
        if (this.getGalleryViewMode() !== galleryViewMode) {
            setGalleryViewMode([{name: "galleryViewMode", value: galleryViewMode}]);
        }
    },
    jumpToGalleryImage(thumbnails, selectedThumbnail, delta, vertical) {
        let currentIndex;
        let selectableThumbnails;
        if (vertical) {
            let posX = selectedThumbnail.offset().left;
            selectableThumbnails = $([]);
            thumbnails.each(function () {
                if (posX === $(this).offset().left) {
                    selectableThumbnails = selectableThumbnails.add($(this));
                }
            });
            currentIndex = selectableThumbnails.get().indexOf(selectedThumbnail[0]);
        } else {
            selectableThumbnails = thumbnails;
            currentIndex = thumbnails.index(selectedThumbnail);
        }
        let newIndex = currentIndex + delta;
        if (currentIndex >= 0 && newIndex >= 0 && newIndex < selectableThumbnails.length) {
            metadataEditor.select(selectableThumbnails[newIndex].dataset.order, selectableThumbnails[newIndex].dataset.stripe, "default");
            let galleryViewMode = this.getGalleryViewMode();
            if (galleryViewMode === "LIST") {
                scrollToStructureThumbnail(selectableThumbnails.eq(newIndex), $("#imagePreviewForm\\:structuredPagesField"));
            } else if (galleryViewMode === "PREVIEW") {
                scrollToPreviewThumbnail(selectableThumbnails.eq(newIndex), $("#thumbnailStripeScrollableContent"));
            }
            return true;
        }
        return false;
    },
    jumpToSelectedImage(delta, vertical) {
        let gallery = $("#galleryWrapperPanel");
        let lastSelection = gallery.find(".thumbnail.last-selection + .thumbnail-container");
        if (lastSelection.length === 1) {
            let thumbnails = gallery.find(".thumbnail + .thumbnail-container");
            if (delta > 0) {
                for (; delta > 0; delta--) {
                    if (vertical && this.jumpToGalleryImage(thumbnails, lastSelection, delta, true)) {
                        break;
                    } else if (!vertical && this.jumpToGalleryImage(thumbnails, lastSelection, delta, false)) {
                        break;
                    }
                }
            } else if (delta < 0) {
                for (; delta < 0; delta++) {
                    if (vertical && this.jumpToGalleryImage(thumbnails, lastSelection, delta, true)) {
                        break;
                    } else if (!vertical && this.jumpToGalleryImage(thumbnails, lastSelection, delta, false)) {
                        break;
                    }
                }
            }
        }
    },
    handleShortcut(shortcut) {
        switch (shortcut) {
            case "help":
                if (!(document.activeElement.tagName === "INPUT" || document.activeElement.tagName === "TEXTAREA")) {
                    PF("helpDialog").show();
                }
                break;
            case "structuredView":
                metadataEditor.shortcuts.changeView("LIST");
                break;
            case "detailView":
                metadataEditor.shortcuts.changeView("PREVIEW");
                break;
            case "nextItem":
                metadataEditor.shortcuts.jumpToSelectedImage(1, false);
                break;
            case "previousItem":
                metadataEditor.shortcuts.jumpToSelectedImage(-1, false);
                break;
            case "nextItemMulti":
                metadataEditor.shortcuts.jumpToSelectedImage(20, false);
                break;
            case "previousItemMulti":
                metadataEditor.shortcuts.jumpToSelectedImage(-20, false);
                break;
            case "downItem":
                metadataEditor.shortcuts.jumpToSelectedImage(1, true);
                break;
            case "upItem":
                metadataEditor.shortcuts.jumpToSelectedImage(-1, true);
                break;
            case "downItemMulti":
                metadataEditor.shortcuts.jumpToSelectedImage(10, true);
                break;
            case "upItemMulti":
                metadataEditor.shortcuts.jumpToSelectedImage(-10, true);
                break;
            default:
                // This default case is only reached when shortcuts exist which are not implemented.
        }
    },
    evaluateKeys(event) {
        Object.keys(metadataEditor.shortcuts.KEYS).forEach((key) => {
            let keyCombination = metadataEditor.shortcuts.KEYS[key];
            if (event.ctrlKey === keyCombination.includes("Control")
                && event.shiftKey === keyCombination.includes("Shift")
                && event.metaKey === keyCombination.includes("Meta")
                && event.altKey === keyCombination.includes("Alt")
                && keyCombination.includes(event.code)
            ) {
                metadataEditor.shortcuts.handleShortcut(key);
                event.preventDefault();
            }
        });
    },
    listen(shortcuts) {
        metadataEditor.shortcuts.KEYS = shortcuts;
        $(document).on("keydown.shortcuts", function (event) {
            metadataEditor.shortcuts.evaluateKeys(event.originalEvent);
        });
    },
    ignore() {
        $(document).off("keydown.shortcuts");
    },
    updateViews() {
        switch (this.getGalleryViewMode()) {
            case "LIST":
                destruct();
                scrollToSelectedThumbnail();
                break;
            case "PREVIEW":
                initialize();
                scrollToSelectedThumbnail();
                changeToMapView();
                break;
        }
    }
};

function expandMetadata(panelClass) {
    $("." + panelClass + "[aria-expanded='false']").click();
}

function setConfirmUnload(on) {
    window.onbeforeunload = (on) ? function() { return true; } : void 0;
}
