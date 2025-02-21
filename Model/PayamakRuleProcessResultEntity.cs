namespace api.Model;

public class PayamakRuleProcessResultEntity
{
    public int Id { get; set; }
    public required PayamakExpectedProcessResultEntity Payamak { get; set; }
    public required string ProcessorId { get; set; }
    public double? Rating { get; set; }
    public required string Comment { get; set; }
    public string? DetailsJson { get; set; }
}