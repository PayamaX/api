namespace PayamaX.Portal.Model;

public class PayamakRuleProcessResultEntity
{
    public virtual int Id { get; set; }
    public virtual required PayamakExpectedProcessResultEntity Payamak { get; set; }
    public virtual required string ProcessorId { get; set; }
    public virtual double? Rating { get; set; }
    public virtual required string Comment { get; set; }
    public virtual string? DetailsJson { get; set; }
}