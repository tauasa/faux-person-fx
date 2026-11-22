package org.tauasa.faux.app;

import org.springframework.stereotype.Service;
import org.tauasa.faux.CompanyGenerator;
import org.tauasa.faux.NameGenerator;
import org.tauasa.faux.PersonGenerator;
import org.tauasa.faux.model.Company;
import org.tauasa.faux.model.Name;
import org.tauasa.faux.model.Person;

/** Turns a {@link GenerateRequest} into rows, through faux-person. */
@Service
public class FauxService {

    private final FauxFxProperties properties;

    public FauxService(FauxFxProperties properties) {
        this.properties = properties;
    }

    /** Generates names. The request's real-companies switch doesn't apply. */
    public Generated<Name> names(GenerateRequest request) {
        checkCount(request);
        NameGenerator.Builder builder = NameGenerator.builder().region(request.region());
        if (request.seed() != null) {
            builder.seed(request.seed());
        }
        NameGenerator generator = builder.build();
        return new Generated<>(generator.next(request.count()), generator.seed());
    }

    /** Generates companies. The request's region doesn't apply. */
    public Generated<Company> companies(GenerateRequest request) {
        checkCount(request);
        CompanyGenerator.Builder builder = CompanyGenerator.builder().realCompanies(request.realCompanies());
        if (request.seed() != null) {
            builder.seed(request.seed());
        }
        CompanyGenerator generator = builder.build();
        return new Generated<>(generator.next(request.count()), generator.seed());
    }

    /** Generates people. */
    public Generated<Person> people(GenerateRequest request) {
        checkCount(request);
        PersonGenerator.Builder builder = PersonGenerator.builder()
                .region(request.region())
                .realCompanies(request.realCompanies());
        if (request.seed() != null) {
            builder.seed(request.seed());
        }
        PersonGenerator generator = builder.build();
        return new Generated<>(generator.next(request.count()), generator.seed());
    }

    /**
     * Makes one of each kind, so faux-person reads its data files now rather
     * than on the first Generate.
     */
    public void warmUp() {
        GenerateRequest one = new GenerateRequest(1, null, 0L, false);
        names(one);
        companies(one);
        people(one);
    }

    /** The most rows one request may ask for. */
    public int maxCount() {
        return properties.maxCount();
    }

    private void checkCount(GenerateRequest request) {
        if (request.count() > properties.maxCount()) {
            throw new IllegalArgumentException(
                    "count must be at most " + properties.maxCount() + ": " + request.count());
        }
    }
}
