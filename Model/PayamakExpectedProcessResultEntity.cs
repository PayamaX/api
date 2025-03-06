using System.ComponentModel.DataAnnotations;
using PayamaX.Portal.Commons;
using PayamaX.Portal.UseCases.UploadPayamak;

namespace PayamaX.Portal.Model;

/// <summary>
/// 
/// </summary>
public class PayamakExpectedProcessResultEntity
{
    /// <summary>
    /// 
    /// </summary>
    public virtual long Id { get; set; }
    
    /// <summary>
    /// 
    /// </summary>
    public virtual required PayamakOriginComponent Origin { get; set; }
    
    /// <summary>
    /// 
    /// </summary>
    [StringLength(4001)]
    public virtual string? BodyText { get; set; }
    
    /// <summary>
    /// 
    /// </summary>
    public virtual required string BodyHash { get; set; }
    
    /// <summary>
    /// 
    /// </summary>
    public virtual long ReceivedEpochMillis { get; set; }
    
    /// <summary>
    /// 
    /// </summary>
    public virtual PayamakUsabilityClass DetectedUsabilityClass{ get; set; }
    
    /// <summary>
    /// 
    /// </summary>
    public virtual PayamakUsabilityClass ExpectedUsabilityClass{ get; set; }

    /// <summary>
    /// 
    /// </summary>
    /// <returns></returns>
    /// <exception cref="NotImplementedException"></exception>
    public virtual PayamakExpectedProcessResultPortable Portable()
    {
        throw new NotImplementedException();
    }
}