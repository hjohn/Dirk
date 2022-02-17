package hs.ddif.core.definition;

import hs.ddif.core.definition.bind.BindingException;

import java.lang.reflect.Type;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Template for constructing {@link Injectable}s.
 *
 * @param <D> type of data resulting from the analysis phase
 */
public interface ClassInjectableFactoryTemplate<D> {

  /**
   * Analyzes the given {@link Type}, potentially doing some preliminary work towards
   * creating a {@link Injectable}.
   *
   * @param type a {@link Type}, cannot be {@code null}
   * @return a {@link TypeAnalysis}, never {@code null}
   */
  TypeAnalysis<D> analyze(Type type);

  /**
   * Creates a {@link Injectable} based on a positive {@link TypeAnalysis} and no
   * further problems are encountered. Supplying a negative analysis will always result in
   * an exception to be thrown.
   *
   * @param analysis a positive {@link TypeAnalysis}, never {@code null}
   * @return a {@link Injectable}, never {@code null}
   * @throws BindingException when an exception occurred while creating a binding
   * @throws UninjectableTypeException when the given {@link Type} in the analysis is not suitable for injection
   */
  Injectable create(TypeAnalysis<D> analysis) throws BindingException, UninjectableTypeException;

  /**
   * Result class for the analysis phase.
   *
   * @param <D> type of data contained in the analysis
   */
  public class TypeAnalysis<D> {
    /**
     * Constructs an analysis with a negative result.
     *
     * @param <D> type of data contained in the analysis
     * @param unsuitableReasonTemplate a format template where parameter 1 is the type, cannot be {@code null}
     * @param parameters additional parameters (parameters 2 and further), can be empty
     * @return a {@link TypeAnalysis}, never {@code null}
     */
    public static <D> TypeAnalysis<D> negative(String unsuitableReasonTemplate, Object... parameters) {
      return new TypeAnalysis<>(unsuitableReasonTemplate, parameters);
    }

    /**
     * Constructs an analysis with a positive result.
     *
     * @param <D> type of data contained in the analysis
     * @param preliminaryData optional preliminary data to be used for the next phase, can be {@code null}
     * @return a {@link TypeAnalysis}, never {@code null}
     */
    public static <D> TypeAnalysis<D> positive(D preliminaryData) {
      return new TypeAnalysis<>(preliminaryData);
    }

    private final String unsuitableReasonTemplate;
    private final Object[] parameters;
    private final D preliminaryData;

    private TypeAnalysis(String unsuitableReasonTemplate, Object... parameters) {
      if(unsuitableReasonTemplate == null) {
        throw new IllegalArgumentException("unsuitableReasonTemplate cannot be null");
      }

      this.unsuitableReasonTemplate = unsuitableReasonTemplate;
      this.parameters = parameters;
      this.preliminaryData = null;
    }

    private TypeAnalysis(D preliminaryData) {
      this.unsuitableReasonTemplate = null;
      this.parameters = null;
      this.preliminaryData = preliminaryData;
    }

    /**
     * Returns {@code true} if the input is not suitable for further processing.
     *
     * @return {@code true} if the input is not suitable for further processing, otherwise {@code false}
     */
    public boolean isNegative() {
      return unsuitableReasonTemplate != null;
    }

    /**
     * If the analysis was negative, returns the reason the type is unsuitable.
     *
     * @param type a {@link Type} to use to construct the message with, cannot be {@code null}
     * @return a message describing why the type was deemed unsuitable, never {@code null}
     */
    public String getUnsuitableReason(Type type) {
      return String.format(unsuitableReasonTemplate, ArrayUtils.insert(0, parameters, type));
    }

    /**
     * Returns the data associated with this analysis.
     *
     * @return the data associated with this analysis, can be {@code null}
     */
    public D getData() {
      return preliminaryData;
    }
  }
}
