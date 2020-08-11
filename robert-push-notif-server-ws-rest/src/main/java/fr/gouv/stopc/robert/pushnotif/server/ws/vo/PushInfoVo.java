package fr.gouv.stopc.robert.pushnotif.server.ws.vo;

import javax.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PushInfoVo {

    @NotBlank(message = "Token is mandatory")
    @ToString.Exclude
    private String token;

    @NotBlank(message = "Locale is mandatory")
    @ToString.Exclude
    private String locale;

    @NotBlank(message = "Timezone is mandatory")
    @ToString.Exclude
    private String timezone;

}
