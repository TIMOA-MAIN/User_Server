package TIMOA.userserver.entity.Dashboard;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
@Table(name = "board_cards")
public class Dashboard_cards {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Long cardId;

    @Column(name = "column_id", nullable = false, insertable = false, updatable = false)
    private Long columnId;

    @Column(name = "description")
    private String description;

    @Column(name = "position_order", nullable = false)
    private Integer positionOrder;

    @Column(name = "labels")
    private String labels;

    // 외래키 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_id")
    private Dashboard_columns boardColumns;

    // 기본 생성자
    public Dashboard_cards() {}

    // 생성자
    public Dashboard_cards(Long cardId, Long columnId, String description, Integer positionOrder, String labels) {
        this.cardId = cardId;
        this.columnId = columnId;
        this.description = description;
        this.positionOrder = positionOrder;
        this.labels = labels;
    }
}
