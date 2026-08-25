package com.readingledger.dataloader;

import com.readingledger.domain.EvidenceDirection;
import com.readingledger.domain.InterpretationThread;
import com.readingledger.domain.PassageAnchor;
import com.readingledger.domain.TextEdition;
import com.readingledger.repository.InterpretationThreadRepository;
import com.readingledger.repository.PassageAnchorRepository;
import com.readingledger.repository.TextEditionRepository;
import com.readingledger.service.HashService;
import com.readingledger.web.dto.CreateRevisionRequest;
import com.readingledger.web.dto.EvidenceInput;
import com.readingledger.service.RevisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class SyntheticDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SyntheticDataLoader.class);

    private final TextEditionRepository editionRepository;
    private final PassageAnchorRepository anchorRepository;
    private final InterpretationThreadRepository threadRepository;
    private final RevisionService revisionService;
    private final HashService hashService;
    private final Clock clock;
    private final boolean enabled;

    public SyntheticDataLoader(TextEditionRepository editionRepository,
                               PassageAnchorRepository anchorRepository,
                               InterpretationThreadRepository threadRepository,
                               RevisionService revisionService,
                               HashService hashService,
                               Clock clock,
                               @Value("${app.synthetic-data.enabled:false}") boolean enabled) {
        this.editionRepository = editionRepository;
        this.anchorRepository = anchorRepository;
        this.threadRepository = threadRepository;
        this.revisionService = revisionService;
        this.hashService = hashService;
        this.clock = clock;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        if (editionRepository.count() > 0) {
            log.info("Synthetic data already present, skipping data loader.");
            return;
        }

        log.info("Loading synthetic Dream of the Red Chamber sample data...");

        Instant now = Instant.now(clock);

        String sourceText = buildSyntheticSourceText();

        TextEdition edition = new TextEdition(
                UUID.randomUUID(),
                "红楼梦（合成片段辑）",
                "SYNTHETIC-EDITION-v1 — 非真实页码，仅供演示",
                sourceText,
                now
        );
        editionRepository.save(edition);

        PassageAnchor anchor1 = createAnchor(edition.getId(), "SYN-CH03", 1, 0, 89,
                "林黛玉初入荣国府，暗自告诫言行举止须时时留意，不肯轻易多说一句话。");
        PassageAnchor anchor2 = createAnchor(edition.getId(), "SYN-CH05", 2, 0, 95,
                "贾宝玉在幻境中翻阅图册，见诗句暗含园中女子命运，却浑然不解其意。");
        PassageAnchor anchor3 = createAnchor(edition.getId(), "SYN-CH27", 3, 0, 87,
                "黛玉荷锄葬花，吟出质本洁来还洁去之叹，感伤春色易逝、身世飘零。");
        PassageAnchor anchor4 = createAnchor(edition.getId(), "SYN-CH74", 4, 0, 92,
                "王夫人命人抄检大观园，晴雯当众将箱笼掀开，以示清白无讳。");
        PassageAnchor anchor5 = createAnchor(edition.getId(), "SYN-CH120", 5, 0, 76,
                "宝玉于雪天拜别父亲，随一僧一道飘然而去，只落得白茫茫大地真干净。");

        InterpretationThread thread = new InterpretationThread(
                UUID.randomUUID(),
                anchor1.getId(),
                "黛玉进府时的心理状态解读",
                now
        );
        threadRepository.save(thread);

        CreateRevisionRequest rev1 = new CreateRevisionRequest(
                "黛玉步步留心、时时在意，表现出寄人篱下的谨慎与自尊。",
                null,
                List.of(new EvidenceInput(anchor1.getId(), EvidenceDirection.SUPPORTS))
        );
        var rev1Response = revisionService.createRevision(thread.getId(), rev1);

        CreateRevisionRequest rev2 = new CreateRevisionRequest(
                "黛玉的谨慎不仅是寄人篱下，更是其清醒自我意识的体现——她主动选择以沉默自持。",
                rev1Response.revisionId(),
                List.of(
                        new EvidenceInput(anchor1.getId(), EvidenceDirection.SUPPORTS),
                        new EvidenceInput(anchor3.getId(), EvidenceDirection.QUALIFIES)
                )
        );
        var rev2Response = revisionService.createRevision(thread.getId(), rev2);

        CreateRevisionRequest rev3 = new CreateRevisionRequest(
                "重新审视后认为，黛玉的谨慎主要源于外来者的生存策略，而非抽象的自我意识；葬花一节显示其感伤更甚于自持。",
                rev2Response.revisionId(),
                List.of(
                        new EvidenceInput(anchor1.getId(), EvidenceDirection.SUPPORTS),
                        new EvidenceInput(anchor3.getId(), EvidenceDirection.CHALLENGES)
                )
        );
        revisionService.createRevision(thread.getId(), rev3);

        log.info("Synthetic data loaded: 1 edition, 5 anchors, 1 thread with 3 revisions.");
    }

    private PassageAnchor createAnchor(UUID editionId, String pageLabel, int paragraphOrder,
                                       int charStart, int charEnd, String snippet) {
        String hash = hashService.sha256(snippet);
        PassageAnchor anchor = new PassageAnchor(
                UUID.randomUUID(),
                editionId,
                pageLabel,
                paragraphOrder,
                charStart,
                charEnd,
                snippet,
                hash,
                Instant.now(clock)
        );
        anchorRepository.save(anchor);
        return anchor;
    }

    private String buildSyntheticSourceText() {
        return String.join("\n\n",
                "林黛玉初入荣国府，暗自告诫言行举止须时时留意，不肯轻易多说一句话。",
                "贾宝玉在幻境中翻阅图册，见诗句暗含园中女子命运，却浑然不解其意。",
                "黛玉荷锄葬花，吟出质本洁来还洁去之叹，感伤春色易逝、身世飘零。",
                "王夫人命人抄检大观园，晴雯当众将箱笼掀开，以示清白无讳。",
                "宝玉于雪天拜别父亲，随一僧一道飘然而去，只落得白茫茫大地真干净。"
        );
    }
}
