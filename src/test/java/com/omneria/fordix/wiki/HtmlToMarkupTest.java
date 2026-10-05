package com.omneria.fordix.wiki;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HtmlToMarkupTest {
    private static HtmlToMarkup.Result convert(String html, List<HtmlToMarkup.Image> seen) {
        return HtmlToMarkup.convert(html, image -> {
            seen.add(image);
            return "img" + seen.size() + ".png";
        });
    }

    @Test
    void headingsParagraphsAndBullets() {
        String html = "<div><p>Le <b>Naga</b> est un boss &amp; un serpent.</p><h2>Combat <span class=\"mw-editsection\">[edit]</span></h2>"
                + "<ul><li>Attaque <a href=\"/wiki/Charge\">charge</a></li><li>Defense</li></ul><h3>Butin</h3><p>Ecailles.</p></div>";
        HtmlToMarkup.Result result = convert(html, new ArrayList<>());
        assertEquals("Le Naga est un boss & un serpent.\n\n# Combat\n\n- Attaque charge\n\n- Defense\n\n## Butin\n\nEcailles.", result.markup());
        assertEquals(List.of("Charge"), result.links());
    }

    @Test
    void dropsNavigationReferencesAndLongSections() {
        String html = "<p>Texte.<sup class=\"reference\"><a href=\"#cite\">[1]</a></sup></p><div class=\"navbox\"><p>Navigation</p></div>"
                + "<h2>History</h2><p>Longue histoire.</p><h3>Details</h3><p>Plus.</p><h2>Utilisation</h2><p>A garder.</p>"
                + "<script>alert(1)</script><style>p{}</style>";
        String markup = convert(html, new ArrayList<>()).markup();
        assertEquals("Texte.\n\n# Utilisation\n\nA garder.", markup);
    }

    @Test
    void tablesBecomeOneBulletPerRow() {
        String html = "<table class=\"infobox-rows\"><tr><th>Sante</th><td>40</td></tr><tr><td>Nom</td><td>Naga</td><td>Boss</td></tr></table>";
        assertEquals("- Sante : 40\n\n- Nom | Naga | Boss", convert(html, new ArrayList<>()).markup());
    }

    @Test
    void picturesAreOfferedToTheCallerAndSmallIconsAreLeftOut() {
        List<HtmlToMarkup.Image> seen = new ArrayList<>();
        String html = "<p>Voir.</p><img src=\"/images/Naga.png?1\" alt=\"Un Naga\" width=\"160\" height=\"120\">"
                + "<img src=\"/images/icon.png\" width=\"16\" height=\"16\"><img src=\"data:image/gif;base64,AAAA\" data-src=\"https://x.test/lazy.png\" alt=\"Lazy\">";
        HtmlToMarkup.Result result = convert(html, seen);
        assertEquals(2, seen.size());
        assertEquals("/images/Naga.png?1", seen.get(0).src());
        assertEquals("https://x.test/lazy.png", seen.get(1).src());
        assertEquals("Voir.\n\n@image img1.png | Un Naga\n\n@image img2.png | Lazy", result.markup());
    }

    @Test
    void imageNamerCanRefuseAPicture() {
        String html = "<img src=\"/a.png\" width=\"100\" height=\"100\">";
        assertEquals("", HtmlToMarkup.convert(html, image -> null).markup());
    }

    @Test
    void pageOfReadsWikiLinks() {
        assertEquals("Naga", HtmlToMarkup.pageOf("/wiki/Naga"));
        assertEquals("Iron_Sword", HtmlToMarkup.pageOf("/w/Iron_Sword#Crafting"));
        assertEquals("Twilight_Forest", HtmlToMarkup.pageOf("/index.php?title=Twilight_Forest&action=edit"));
        assertEquals("Boss_(Mob)", HtmlToMarkup.pageOf("/wiki/Boss_%28Mob%29"));
        assertNull(HtmlToMarkup.pageOf("/wiki/File:Naga.png"));
        assertNull(HtmlToMarkup.pageOf("/wiki/Category:Mobs"));
        assertNull(HtmlToMarkup.pageOf("https://example.com/page"));
    }

    @Test
    void decodesEntities() {
        assertEquals("a & b < c \u2013 d \u00E9", HtmlToMarkup.decode("a &amp; b &lt; c &ndash; d &#233;"));
        assertTrue(HtmlToMarkup.decode("&unknown;").contains("&unknown;"));
        assertFalse(HtmlToMarkup.convert("<p>&nbsp;</p>", null).markup().contains("&"));
    }
}
