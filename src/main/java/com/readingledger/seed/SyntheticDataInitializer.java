package com.readingledger.seed;

import com.readingledger.repo.TextEditionRepository;
import com.readingledger.service.AnchorService;
import com.readingledger.service.EditionService;
import com.readingledger.service.Sha256;
import com.readingledger.web.dto.CreateAnchorRequest;
import com.readingledger.web.dto.CreateEditionRequest;
import com.readingledger.web.dto.EditionResponse;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 合成演示数据：登记一个《红楼梦》演示版本与若干段落锚点。
 * 注意：所有 pageLabel 都是显式虚构的标签（“合成页·…”），
 * 不对应、也不声称对应任何真实纸质版本的页码。
 */
@Component
public class SyntheticDataInitializer implements ApplicationRunner {

    public static final String SYNTHETIC_EDITION_TITLE =
            "《红楼梦》合成片段集（演示数据·页码均为虚构）";

    private static final String EXCERPT_ONE =
            "此开卷第一回也。作者自云：因曾历过一番梦幻之后，故将真事隐去，"
                    + "而借通灵之说，撰此《石头记》一书也。";

    private static final String EXCERPT_TWO =
            "宝玉看罢，因笑道：这个妹妹我曾见过的。贾母笑道：又胡说了，你何曾见过她？"
                    + "宝玉笑道：虽然未曾见过她，然我看着面善，心里就算是旧相识。";

    private static final String EXCERPT_THREE =
            "贾不假，白玉为堂金作马。阿房宫，三百里，住不下金陵一个史。"
                    + "东海缺少白玉床，龙王来请金陵王。丰年好大雪，珍珠如土金如铁。";

    private final EditionService editionService;
    private final AnchorService anchorService;
    private final TextEditionRepository editionRepository;

    public SyntheticDataInitializer(EditionService editionService,
                                    AnchorService anchorService,
                                    TextEditionRepository editionRepository) {
        this.editionService = editionService;
        this.anchorService = anchorService;
        this.editionRepository = editionRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (editionRepository.existsByTitle(SYNTHETIC_EDITION_TITLE)) {
            return;
        }
        String syntheticSource = EXCERPT_ONE + EXCERPT_TWO + EXCERPT_THREE;
        EditionResponse edition = EditionResponse.from(editionService.register(new CreateEditionRequest(
                SYNTHETIC_EDITION_TITLE,
                "曹雪芹（清）·合成演示文本",
                Sha256.hex(syntheticSource),
                "合成演示数据：pageLabel 均为虚构标签（如“合成页·第一回·片段甲”），"
                        + "不对应任何真实纸质版本的页码；excerptSha256 由服务端对摘录文本计算，"
                        + "sourceTextSha256 为三段合成摘录拼接后的 SHA-256。"
        )));
        seedAnchor(edition.id(), "合成页·第一回·片段甲", 0, EXCERPT_ONE);
        seedAnchor(edition.id(), "合成页·第三回·片段乙", 0, EXCERPT_TWO);
        seedAnchor(edition.id(), "合成页·第四回·片段丙", 0, EXCERPT_THREE);
    }

    private void seedAnchor(UUID editionId, String pageLabel, int paragraphOrdinal, String excerpt) {
        anchorService.register(editionId, new CreateAnchorRequest(
                pageLabel,
                paragraphOrdinal,
                0,
                excerpt.length(),
                excerpt
        ));
    }
}
