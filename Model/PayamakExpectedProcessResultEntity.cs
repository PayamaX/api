using api.Commons;
using api.UseCases.UploadPayamak;

namespace api.Model;

public class PayamakExpectedProcessResultEntity
{
    public int Id { get; set; }
    public required PayamakOrigin Origin { get; set; }
    public string? BodyText { get; set; }
    public string BodyHash { get; set; }
    public long ReceivedEpochMillis { get; set; }
    public PayamakUsabilityClass DetectedUsabilityClass{ get; set; }
    public PayamakUsabilityClass ExpectedUsabilityClass{ get; set; }
}