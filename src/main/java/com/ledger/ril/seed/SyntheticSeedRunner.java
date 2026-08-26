package com.ledger.ril.seed;

import java.util.List;

import com.ledger.ril.api.dto.CreateAnchorRequest;
import com.ledger.ril.api.dto.CreateEditionRequest;
import com.ledger.ril.domain.PassageAnchor;
import com.ledger.ril.domain.TextEdition;
import com.ledger.ril.repo.TextEditionRepository;
import com.ledger.ril.service.LedgerService;
import com.ledger.ril.support.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeds a synthetic edition of 《红楼梦》 (Dream of the Red Chamber) with a handful
 * of passage anchors.
 *
 * <p><strong>Everything here is synthetic.</strong> The passage texts are short
 * paraphrase-style fragments authored for this demo, not transcriptions from any
 * real edition, and the page numbers are invented positional markers — they do
 * NOT correspond to any real printed page in any actual edition. The SHA-256 for
 * each anchor is computed over the synthetic fragment shown in its note, so the
 * hash is internally consistent and can be used to exercise evidence assertions.
 */
@Component
@ConditionalOnProperty(name = "ril.seed.enabled", havingValue = "true", matchIfMissing = true)
public class SyntheticSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SyntheticSeedRunner.class);

    private static final String EDITION_TITLE = "《红楼梦》(合成演示本 / Synthetic Demo Edition)";
    private static final String EDITOR_LABEL = "RIL Synthetic Fixtures";

    /** A synthetic fragment: invented page marker, paragraph ordinal, char range, and demo text. */
    private record Fragment(int page, int paragraph, int charStart, int charEnd, String label, String text) {
    }

    // NOTE: page numbers are synthetic positional markers, not real edition pages.
    private static final List<Fragment> FRAGMENTS = List.of(
            new Fragment(1, 0, 0, 48, "太虚幻境·对联",
                    "[synthetic] 假作真时真亦假，无为有处有还无。—— 合成片段，非真实版本文字。"),
            new Fragment(2, 3, 120, 205, "黛玉进府·初见",
                    "[synthetic] 一个眼中含情、步履轻盈的身影初入荣府，众人相看皆有揣度。此为演示用改写片段。"),
            new Fragment(5, 7, 640, 742, "宝玉梦游·判词",
                    "[synthetic] 梦中翻阅金陵册子，判词隐语纷纭，似谶似诗，读者各有其解。合成文本。"),
            new Fragment(8, 1, 12, 96, "宝钗·金锁",
                    "[synthetic] 金锁上錾着的两句吉谶，被有心人反复提起，弦外之音耐人寻味。演示片段。"),
            new Fragment(23, 5, 300, 388, "共读·西厢",
                    "[synthetic] 花下共读一册杂书，词句撩动心事，两人相视无言。此为合成改写，非原文。")
    );

    private final TextEditionRepository editions;
    private final LedgerService ledger;

    public SyntheticSeedRunner(TextEditionRepository editions, LedgerService ledger) {
        this.editions = editions;
        this.ledger = ledger;
    }

    @Override
    public void run(String... args) {
        if (editions.existsByTitleAndEditorLabel(EDITION_TITLE, EDITOR_LABEL)) {
            log.info("Synthetic seed already present; skipping.");
            return;
        }

        TextEdition edition = ledger.createEdition(new CreateEditionRequest(
                EDITION_TITLE,
                EDITOR_LABEL,
                true,
                "Synthetic demo fixtures. Passage texts are paraphrase-style fragments and page "
                        + "numbers are invented positional markers; neither reflects any real edition."));

        for (Fragment f : FRAGMENTS) {
            String versionId = "synthetic-v1";
            String sha256 = Hashing.sha256Hex(f.text());
            PassageAnchor anchor = ledger.createAnchor(edition.getId(), new CreateAnchorRequest(
                    versionId, f.page(), f.paragraph(), f.charStart(), f.charEnd(), sha256, f.label()));
            log.info("Seeded synthetic anchor {} ({}), source sha256={}",
                    anchor.getId(), f.label(), sha256);
        }

        log.info("Seeded synthetic 《红楼梦》 demo edition {} with {} anchors.",
                edition.getId(), FRAGMENTS.size());
    }
}
