import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

public class CrearPedidoRequest {

    @NotNull
    private Long clienteId;

    @NotEmpty
    @Valid
    private List<LineaPedidoRequest> lineas;

    public CrearPedidoRequest() {
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public List<LineaPedidoRequest> getLineas() {
        return lineas;
    }

    public void setLineas(List<LineaPedidoRequest> lineas) {
        this.lineas = lineas;
    }    











}