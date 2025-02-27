using System.ComponentModel.DataAnnotations;
using PayamaX.Portal.Commons;
using PayamaX.Portal.UseCases.UploadPayamak;

namespace PayamaX.Portal.Model;

public class PayamakExpectedProcessResultEntity
{
    public virtual int Id { get; set; }
    public virtual required PayamakOriginComponent Origin { get; set; }
    
    [StringLength(4001)]
    public virtual string? BodyText { get; set; }
    public virtual required string BodyHash { get; set; }
    public virtual long ReceivedEpochMillis { get; set; }
    public virtual PayamakUsabilityClass DetectedUsabilityClass{ get; set; }
    public virtual PayamakUsabilityClass ExpectedUsabilityClass{ get; set; }

    public virtual PayamakExpectedProcessResultPortable Portable()
    {
        throw new NotImplementedException();
    }
}