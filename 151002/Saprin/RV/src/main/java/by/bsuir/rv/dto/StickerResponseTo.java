package by.bsuir.rv.dto;

import by.bsuir.rv.bean.IdentifiedBean;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class StickerResponseTo extends IdentifiedBean {
    private String name;
    private BigInteger issueId;

    public StickerResponseTo(BigInteger id, String name, BigInteger issueId) {
        super(id);
        this.name = name;
        this.issueId = issueId;
    }
}
