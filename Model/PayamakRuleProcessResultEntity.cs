namespace PayamaX.Portal.Model;

/// <summary>
/// 
/// </summary>
public class PayamakRuleProcessResultEntity
{
    /// <summary>
    /// 
    /// </summary>
    public virtual long Id { get; set; }
    
    /// <summary>
    /// 
    /// </summary>
    public virtual required PayamakExpectedProcessResultEntity Payamak { get; set; }
    
    /// <summary>
    /// 
    /// </summary>
    public virtual required string ProcessorId { get; set; }
    
    /// <summary>
    /// 
    /// </summary>
    public virtual double? Rating { get; set; }
    
    /// <summary>
    /// 
    /// </summary>
    public virtual required string Comment { get; set; }
    
    /// <summary>
    /// 
    /// </summary>
    public virtual string? DetailsJson { get; set; }
}