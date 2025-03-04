using FluentNHibernate.Conventions;
using FluentNHibernate.Conventions.Instances;

namespace PayamaX.Portal.Config.NHibernateFluentAutoMap;

public class PayamaxConventions : IClassConvention
{
    public void Apply(IClassInstance instance)
    {
        instance.Table($"\"{instance.EntityType.Name[..^"Entity".Length]}\"");
    }
}