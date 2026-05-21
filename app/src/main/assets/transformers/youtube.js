/*
 * Argus Ad Blocker — YouTube InnerTube Response Transformer
 * @host *.youtube.com,*.googleapis.com
 * @path /youtubei/v1/*
 * @content-type application/json,text/json
 *
 * Albania Region Spoof Technique:
 * YouTube serves dramatically fewer ads in low-ad-inventory regions
 * like Albania (AL), Kosovo (XK), and some Eastern European markets.
 * When "Albania Mode" is enabled in the app UI, MitmSocketHandler
 * injects gl=AL / hl=sq into request bodies so YouTube's servers
 * respond with Albanian-region content that has minimal or no ad
 * metadata.
 *
 * Current capabilities (DNS-only mode):
 *   - Strips adPlacements, playerAds, adBreaks from responses
 *   - Removes adRenderer objects recursively
 *   - Removes promotional content panels
 *   - Reads ARGUS_ALBANIA_MODE flag for future request injection
 *
 * Future (requires MITM / Full Tunnel):
 *   - Request body injection for region spoofing
 */

function transform(url, host, path, contentType, body) {
    if (!body || body.trim().charAt(0) !== '{') {
        return body;
    }

    // Read Albania-mode flag injected by QuickJsTransformer
    var albaniaMode = false;
    try {
        if (typeof ARGUS_ALBANIA_MODE !== 'undefined') {
            albaniaMode = !!ARGUS_ALBANIA_MODE;
        }
    } catch (e) {
        // flag not available — normal mode
    }

    try {
        var json = JSON.parse(body);
        var modified = false;

        // ============================================================
        // 1. Strip known top-level ad fields
        // ============================================================
        var adFields = [
            "adPlacements", "playerAds", "adBreaks",
            "adSlots", "promotionalItems",
            "playerConfig", // sometimes contains ad signals
            "trackingParams"
        ];
        for (var i = 0; i < adFields.length; i++) {
            if (json[adFields[i]] !== undefined) {
                delete json[adFields[i]];
                modified = true;
            }
        }

        // ============================================================
        // 2. Recursively remove objects containing adRenderer
        // ============================================================
        function removeAds(obj) {
            if (typeof obj !== 'object' || obj === null) return false;
            var changed = false;
            if (Array.isArray(obj)) {
                for (var j = obj.length - 1; j >= 0; j--) {
                    if (typeof obj[j] === 'object' && obj[j] !== null &&
                        (obj[j].adRenderer || obj[j].promotionalVideoRenderer ||
                         obj[j].mastheadAdRenderer || obj[j].engagementPanelAds)) {
                        obj.splice(j, 1);
                        changed = true;
                    } else {
                        if (removeAds(obj[j])) changed = true;
                    }
                }
            } else {
                var keys = Object.keys(obj);
                for (var k = 0; k < keys.length; k++) {
                    var key = keys[k];
                    var val = obj[key];
                    if (typeof val === 'object' && val !== null &&
                        (val.adRenderer || val.promotionalVideoRenderer ||
                         val.mastheadAdRenderer || val.engagementPanelAds)) {
                        delete obj[key];
                        changed = true;
                    } else {
                        if (removeAds(val)) changed = true;
                    }
                }
            }
            return changed;
        }

        if (removeAds(json)) modified = true;

        // ============================================================
        // 3. Strip engagementPanels with ad identifiers
        // ============================================================
        if (json.engagementPanels && Array.isArray(json.engagementPanels)) {
            var beforeLen = json.engagementPanels.length;
            json.engagementPanels = json.engagementPanels.filter(function(panel) {
                var id = panel && panel.engagementPanelSectionListRenderer &&
                         panel.engagementPanelSectionListRenderer.panelIdentifier;
                return !(id && (id.indexOf('ads_') >= 0 || id.indexOf('promotion') >= 0 ||
                                id.indexOf('shopping') >= 0));
            });
            if (json.engagementPanels.length !== beforeLen) modified = true;
        }

        // ============================================================
        // 4. Strip watchNextResponse ad sections (end-screen ads)
        // ============================================================
        if (json.watchNextResponse && json.watchNextResponse.contents &&
            json.watchNextResponse.contents.twoColumnWatchNextResults) {
            var results = json.watchNextResponse.contents.twoColumnWatchNextResults;
            if (results.results && results.results.results &&
                Array.isArray(results.results.results.contents)) {
                var beforeLen2 = results.results.results.contents.length;
                results.results.results.contents =
                    results.results.results.contents.filter(function(item) {
                        return !(item && item.itemSectionRenderer &&
                                 item.itemSectionRenderer.sectionIdentifier ===
                                 "legacy-comments-section-shelf");
                    });
                if (results.results.results.contents.length !== beforeLen2) modified = true;
            }
        }

        // ============================================================
        // 5. Remove adSignals from playbackTracking
        // ============================================================
        if (json.playbackTracking && json.playbackTracking.adSignals) {
            delete json.playbackTracking.adSignals;
            modified = true;
        }

        // ============================================================
        // 6. ALBANIA-MODE: strip additional Albanian-region-specific
        //    ad fields that sometimes survive the generic pass above.
        // ============================================================
        if (albaniaMode) {
            // When Albania mode is on, the server usually returns fewer ads,
            // but some global tracking fields may still be present.
            var extraFields = [
                "attestation",      // ad-fraud verification
                "heartbeatParams",  // ad-view tracking
                "probeUrl",         // ad beacon
                "serverAbrStreamingUrl" // sometimes carries ad insertion params
            ];
            for (var f = 0; f < extraFields.length; f++) {
                if (json[extraFields[f]] !== undefined) {
                    delete json[extraFields[f]];
                    modified = true;
                }
            }
        }

        return modified ? JSON.stringify(json) : body;
    } catch (e) {
        return body;
    }
}
