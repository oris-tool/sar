import math
muA = 1/(0.00615) + 1/(0.01289)
muB = 1/(0.00208) + 1/(0.00277)
sigmaA2 = 1/((0.00615)**2) + 1/((0.01289)**2)
sigmaB2 = 1/((0.00208)**2) + 1/((0.00277)**2)

final = muA + muB + math.sqrt(sigmaA2 + sigmaB2)

print(final)