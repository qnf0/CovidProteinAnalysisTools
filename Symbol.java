package flyhigh;

import java.util.List;

public class Symbol {
String name;
List<GO>annotations;

Status tf;
Status dnaBinding;
Status chromatinRemodeling;
Status histoneComponent;
Status cofactor;
Status insulator;
Status riscRelated;
Status undescribedFunction;
Status functionKnown;
public Symbol(String name, List<GO> annotations, Status tf, Status dnaBinding, Status chromatinRemodeling,
		Status histoneComponent, Status cofactor, Status insulator, Status riscRelated, Status undescribedFunction,
		Status functionKnown) {
	super();
	this.name = name;
	this.annotations = annotations;
	this.tf = tf;
	this.dnaBinding = dnaBinding;
	this.chromatinRemodeling = chromatinRemodeling;
	this.histoneComponent = histoneComponent;
	this.cofactor = cofactor;
	this.insulator = insulator;
	this.riscRelated = riscRelated;
	this.undescribedFunction = undescribedFunction;
	this.functionKnown = functionKnown;
}


/** Build a Symbol using statuses from a Cat row. */
public static Symbol fromCat(String name, List<GO> annotations, Cat c) {
    return new Symbol(
        name,
        annotations,
        c.tf, c.dnaBinding, c.chromatinRemodeling, c.histoneComponent,
        c.cofactor, c.insulator, c.riscRelated, c.undescribedFunction, c.functionKnown
    );
}



@Override
public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Symbol: ").append(name).append("\n");
    sb.append("Annotations:\n");

    if (annotations != null && !annotations.isEmpty()) {
        for (GO go : annotations) {
            sb.append("  - ").append(go.toString()).append("\n");
        }
    } else {
        sb.append("  (none)\n");
    }

    sb.append("Functional Statuses:\n");
    sb.append("  TF: ").append(tf).append("\n");
    sb.append("  DNA Binding: ").append(dnaBinding).append("\n");
    sb.append("  Chromatin Remodeling: ").append(chromatinRemodeling).append("\n");
    sb.append("  Histone Component: ").append(histoneComponent).append("\n");
    sb.append("  Cofactor: ").append(cofactor).append("\n");
    sb.append("  Insulator: ").append(insulator).append("\n");
    sb.append("  RISC Related: ").append(riscRelated).append("\n");
    sb.append("  Undescribed Function: ").append(undescribedFunction).append("\n");
    sb.append("  Function Known: ").append(functionKnown).append("\n");

    return sb.toString();
}

}
